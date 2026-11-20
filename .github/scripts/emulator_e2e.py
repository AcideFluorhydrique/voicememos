#!/usr/bin/env python3
"""Drive Voice Memos on a running emulator the way a person would and check it keeps working.

Usage: emulator_e2e.py <apk dir> <results dir>

Every APK in the folder goes through the same steps: start, record, pause and resume, discard,
keep recording in the background, use the notification buttons, play and seek, rename,
favourite, search, share, trim, delete by button, by swipe and one this install does not own,
recover and purge in Recently Deleted, every setting, every recording format, rotation, dark
mode, being killed in the background, the deep links and the permissions being refused.

Controls are found the way TalkBack finds them, by their label or by their test tag, which the
app exposes as a resource id. Nothing depends on a screen position. The screen is read with
uiautomator2, the uiautomator command waits for the screen to be still for a second and the
recorder's timer never is.

Each step leaves a screenshot. After each step the crash buffer is read, a crash fails that
step and the app is started again, so one problem does not hide the steps after it. The
results dir gets one folder per APK with the screenshots, the UI of every failed step, the
logcat and a summary, and the summary is also written to the GitHub job summary.

The exit status is non zero if any step failed on any APK.
"""

import math
import os
import re
import shlex
import struct
import subprocess
import sys
import tempfile
import time
import traceback
import wave
import xml.etree.ElementTree as ET

APK_DIR, OUT_ROOT = sys.argv[1], sys.argv[2]

SEED_TITLE = 'Left behind'

# ---------------------------------------------------------------------------------------------
# adb


def adb(*args, check=False, timeout=120):
	r = subprocess.run(['adb', *args], capture_output=True, text=True, timeout=timeout)
	if check and r.returncode:
		raise RuntimeError(f'adb {" ".join(args)}: {r.stdout}{r.stderr}')
	return r.stdout


def sh(command, timeout=120):
	return adb('shell', command, timeout=timeout)


def keyevent(*codes):
	sh('input keyevent ' + ' '.join(str(c) for c in codes))
	time.sleep(.8)


def back():
	keyevent('KEYCODE_BACK')


def home():
	keyevent('KEYCODE_HOME')


API = int(sh('getprop ro.build.version.sdk').strip())
# the app records into Recordings from android 12, into Music before that
FOLDER = '/sdcard/Recordings/RecorderApp' if API >= 31 else '/sdcard/Music/RecorderApp'
RELATIVE = ('Recordings' if API >= 31 else 'Music') + '/RecorderApp/'

BUILD_TOOLS = sorted(
	(os.path.join(os.environ['ANDROID_HOME'], 'build-tools', d)
	 for d in os.listdir(os.path.join(os.environ['ANDROID_HOME'], 'build-tools'))),
	key=lambda p: [int(x) if x.isdigit() else 0 for x in re.split(r'[.-]', os.path.basename(p))],
)[-1]


def package_of(apk):
	return subprocess.run([os.path.join(BUILD_TOOLS, 'aapt2'), 'dump', 'packagename', apk],
	                      capture_output=True, text=True).stdout.strip()


# ---------------------------------------------------------------------------------------------
# the screen


class Node:
	def __init__(self, el, window=0):
		self.window = window
		self.text = el.get('text', '')
		self.desc = el.get('content-desc', '')
		self.rid = el.get('resource-id', '')
		self.cls = el.get('class', '')
		self.package = el.get('package', '')
		self.checked = el.get('checked') == 'true'
		self.selected = el.get('selected') == 'true'
		self.enabled = el.get('enabled') != 'false'
		m = re.match(r'\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]', el.get('bounds', ''))
		self.x1, self.y1, self.x2, self.y2 = map(int, m.groups()) if m else (0, 0, 0, 0)

	@property
	def center(self):
		return (self.x1 + self.x2) // 2, (self.y1 + self.y2) // 2

	@property
	def label(self):
		return self.text or self.desc

	def __repr__(self):
		return f'<{self.cls.split(".")[-1]} text={self.text!r} desc={self.desc!r} id={self.rid!r}>'


LAST_XML = ['']


def parse(xml):
	"""The nodes of a dump, each knowing which window it is in, the top level nodes are windows."""
	LAST_XML[0] = xml
	root = ET.fromstring(xml.encode('utf-8') if isinstance(xml, str) else xml)
	nodes = []
	for window, top in enumerate(root.findall('node')):
		nodes += [Node(el, window) for el in top.iter('node')]
	return nodes


def _connect_u2():
	try:
		import uiautomator2
		device = uiautomator2.connect()
		device.dump_hierarchy()
		return device
	except Exception as e:
		print(f'uiautomator2 is not usable, falling back to the uiautomator command: {e}', flush=True)
		return None


U2 = _connect_u2()


def dump():
	"""Every node on screen, the app's windows and the system's alike."""
	if U2 is not None:
		for _ in range(4):
			try:
				return parse(U2.dump_hierarchy(compressed=False))
			except Exception as e:
				print(f'  dump failed: {e}', flush=True)
				time.sleep(1)
		return []
	for _ in range(4):
		sh('rm -f /sdcard/ui.xml')
		sh('uiautomator dump /sdcard/ui.xml', timeout=60)
		xml = sh('cat /sdcard/ui.xml')
		if '<hierarchy' in xml:
			try:
				return parse(xml[xml.index('<?xml'):] if '<?xml' in xml else xml)
			except ET.ParseError:
				pass
		# the dump gives up while the screen is still animating, give it a moment
		time.sleep(1)
	return []


def rid_matches(node, rid):
	# a test tag comes through as is, a view id as package:id/name
	return node.rid == rid or node.rid.endswith(':id/' + rid)


def matches(node, text=None, desc=None, rid=None, label=None, contains=None, package=None,
            cls=None):
	if cls is not None and node.cls != cls:
		return False
	# a clickable node whose text got clipped away still carries its label as description
	if text is not None and node.text.casefold() != text.casefold() \
			and not (not node.text and node.desc.casefold() == text.casefold()):
		return False
	if desc is not None and node.desc.casefold() != desc.casefold():
		return False
	if rid is not None and not rid_matches(node, rid):
		return False
	if label is not None and label.casefold() not in (node.text.casefold(), node.desc.casefold()):
		return False
	if contains is not None and contains.lower() not in (node.text + ' ' + node.desc).lower():
		return False
	if package is not None and node.package != package:
		return False
	return True


def find(nodes=None, **query):
	nodes = dump() if nodes is None else nodes
	for node in nodes:
		if matches(node, **query):
			return node
	return None


def find_all(nodes=None, **query):
	nodes = dump() if nodes is None else nodes
	return [node for node in nodes if matches(node, **query)]


def wait_for(timeout=15, gone=False, **query):
	"""The node once it is on screen, or True once it has left when gone is set."""
	deadline = time.time() + timeout
	while True:
		node = find(**query)
		if gone and node is None:
			return True
		if not gone and node is not None:
			return node
		if time.time() > deadline:
			what = ', '.join(f'{k}={v!r}' for k, v in query.items())
			raise AssertionError(f'{"still showing" if gone else "never showed"}: {what}')
		time.sleep(.7)


def tap_node(node, pause=1.2):
	x, y = node.center
	sh(f'input tap {x} {y}')
	time.sleep(pause)


def bounds(node):
	return node.x1, node.y1, node.x2, node.y2


def tap(timeout=15, pause=1.2, **query):
	"""Tap a node once it stopped moving.

	Sheets slide up and dialogs move with the keyboard, a tap on where a control was a
	moment ago lands on the scrim and dismisses the sheet or the dialog instead.
	"""
	node = wait_for(timeout=timeout, **query)
	for _ in range(8):
		time.sleep(.35)
		again = find(**query)
		if again is not None and bounds(again) == bounds(node):
			break
		node = again or node
	tap_node(node, pause)
	return node


def tap_in_dialog(title, text, timeout=15):
	"""Tap text in the dialog headed title, not a control of the same name behind it."""
	deadline = time.time() + timeout
	while True:
		nodes = dump()
		heading = find(nodes, text=title)
		candidates = find_all(nodes, text=text) or find_all(nodes, desc=text)
		if heading is not None:
			# the dialog is a window of its own, a control with the same label behind it is not
			own = [n for n in candidates if n.window == heading.window]
			below = [n for n in (own or candidates) if n.y1 > heading.y1] or own
		else:
			below = []
		if below:
			target = min(below, key=lambda n: n.y1 - heading.y1)
			time.sleep(.6)
			# the keyboard moves the dialog up, a tap on where the button was misses the
			# dialog and dismisses it, only tap a button that stayed in place
			again = [n for n in find_all(text=text) + find_all(desc=text)
			         if (n.x1, n.y1, n.x2, n.y2) == (target.x1, target.y1, target.x2, target.y2)]
			if again:
				print(f'  dialog {title!r}: tapping {text!r} at {bounds(target)}', flush=True)
				tap_node(target)
				return
		if time.time() > deadline:
			raise AssertionError(f'no {text!r} under {title!r}')
		time.sleep(.7)


def tap_lowest(text, timeout=15):
	"""Tap the lowest node showing text, the one in a sheet rising from the bottom."""
	wait_for(text=text, timeout=timeout)
	node = max(find_all(text=text), key=lambda n: n.y1)
	# the sheet slides up, wait for it to stop before tapping
	for _ in range(8):
		time.sleep(.35)
		found = find_all(text=text)
		again = max(found, key=lambda n: n.y1) if found else node
		if bounds(again) == bounds(node):
			break
		node = again
	tap_node(node)


def swipe(x1, y1, x2, y2, ms=350):
	sh(f'input swipe {int(x1)} {int(y1)} {int(x2)} {int(y2)} {ms}')
	time.sleep(1.2)


def screen_size():
	m = re.search(r'(\d+)x(\d+)', sh('wm size'))
	return tuple(map(int, m.groups())) if m else (1080, 2400)


def type_text(text):
	# input text takes %s for a space and nothing that the shell would read
	sh('input text ' + shlex.quote(text.replace(' ', '%s')))
	time.sleep(.8)


def clear_field():
	keyevent('KEYCODE_MOVE_END')
	sh('input keyevent ' + ' '.join(['KEYCODE_DEL'] * 60))
	time.sleep(.5)


def hide_keyboard():
	if 'mInputShown=true' in sh('dumpsys input_method'):
		back()


def allow_system_dialog(timeout=8):
	"""Accept whatever the system asks, a permission or media store's write approval."""
	deadline = time.time() + timeout
	while time.time() < deadline:
		nodes = dump()
		for node in nodes:
			if node.package == PKG:
				continue
			if (rid_matches(node, 'permission_allow_button')
					or rid_matches(node, 'permission_allow_foreground_only_button')
					or re.fullmatch(r'(?i)allow|while using the app', node.text)):
				tap_node(node)
				return True
		time.sleep(1)
	return False


def deny_system_dialog(timeout=8):
	deadline = time.time() + timeout
	while time.time() < deadline:
		for node in dump():
			if node.package == PKG:
				continue
			if (rid_matches(node, 'permission_deny_button')
					or rid_matches(node, 'permission_deny_and_dont_ask_again_button')
					or re.fullmatch(r"(?i)deny|don.t allow", node.text)):
				tap_node(node)
				return True
		time.sleep(1)
	return False


# ---------------------------------------------------------------------------------------------
# the app


def launch():
	sh(f'monkey -p {PKG} -c android.intent.category.LAUNCHER 1')
	time.sleep(2)


def is_running():
	return bool(sh(f'pidof {PKG}').strip())


def at_home(nodes=None):
	nodes = dump() if nodes is None else nodes
	return (find(nodes, rid='record_button') is not None
	        or find(nodes, text='Allow Access') is not None)


def settled_at_home(nodes):
	return (at_home(nodes) and find(nodes, text='New Recording') is None
	        and find(nodes, text='Cancel') is None
	        and find(nodes, package='com.android.systemui', rid='notification_text') is None)


def go_home(timeout=40):
	"""Back to the memos list from wherever a failed step left the app, by force if need be."""
	sh('cmd statusbar collapse')
	hide_keyboard()
	deadline = time.time() + timeout / 2
	while time.time() < deadline:
		nodes = dump()
		if find(nodes, package=PKG) is None or not is_running():
			launch()
			continue
		if settled_at_home(nodes):
			# a recording a failed step left running would change every count after it
			if find(nodes, rid='recording_indicator') is not None:
				tap(rid='record_button')
				time.sleep(2)
				continue
			return
		back()
	print('  forcing the app to stop to get back to the memos list', flush=True)
	sh(f'am force-stop {PKG}')
	time.sleep(1)
	launch()
	deadline = time.time() + timeout / 2
	while time.time() < deadline:
		if settled_at_home(dump()):
			return
		time.sleep(1)
	raise AssertionError('could not get back to the memos list')


def recordings():
	"""What media store holds in the app folder, as {display name: duration in ms}."""
	where = f"relative_path LIKE '{RELATIVE}%'"
	out = sh('content query --uri content://media/external/audio/media '
	         f'--projection _id:_display_name:duration --where {shlex.quote(where)}')
	found = {}
	for line in out.splitlines():
		m = re.search(r'_id=(\d+), _display_name=(.*), duration=(\S*)$', line.strip())
		if m:
			found[m.group(2)] = (int(m.group(1)), int(m.group(3)) if m.group(3).isdigit() else 0)
	return found


def wait_new_recording(before, timeout=20):
	"""The display name of the recording that showed up since before, with a duration."""
	deadline = time.time() + timeout
	while time.time() < deadline:
		now = recordings()
		new = {k: v for k, v in now.items() if k not in before}
		for name, (_, duration) in new.items():
			if duration > 0:
				return name
		time.sleep(1)
	raise AssertionError(f'no new recording with a duration, before {sorted(before)}, '
	                     f'after {sorted(recordings())}')


def memo_titles(nodes=None):
	"""Titles of the rows on screen, the first text line of every memo row."""
	nodes = dump() if nodes is None else nodes
	rows = find_all(nodes, rid='memo_row')
	texts = [n for n in nodes if n.text and n.cls.endswith('TextView')]
	titles = []
	for row in rows:
		inside = [t for t in texts if row.x1 <= t.center[0] <= row.x2 and row.y1 <= t.center[1] <= row.y2]
		inside.sort(key=lambda t: t.y1)
		if row.text:
			titles.append(row.text.split(',')[0].strip())
		elif inside:
			titles.append(inside[0].text)
	return titles


def memo_row(title):
	nodes = dump()
	for row in find_all(nodes, rid='memo_row'):
		# a row reads as its title, date and length joined with commas
		if row.text.split(', ')[0] == title:
			return row
		for t in nodes:
			if t.text == title and row.y1 <= t.center[1] <= row.y2:
				return row
	return None


def scroll_list(down):
	w, h = screen_size()
	if down:
		swipe(w / 2, h * .7, w / 2, h * .35, ms=500)
	else:
		swipe(w / 2, h * .35, w / 2, h * .8, ms=300)


def find_memo_anywhere(title):
	"""The row of title, scrolling the list from the top to the bottom to find it."""
	row = memo_row(title)
	if row is not None:
		return row
	for _ in range(4):
		scroll_list(down=False)
	seen = None
	for _ in range(10):
		row = memo_row(title)
		if row is not None:
			return row
		titles = memo_titles()
		if titles == seen:
			break
		seen = titles
		scroll_list(down=True)
	for _ in range(4):
		scroll_list(down=False)
	return None


def wait_memo(title, timeout=15, gone=False):
	deadline = time.time() + timeout
	while time.time() < deadline:
		row = memo_row(title)
		if gone and row is None and find_memo_anywhere(title) is None:
			return True
		if not gone and row is not None:
			return row
		if not gone and find(rid='memos_list') is not None:
			row = find_memo_anywhere(title)
			if row is not None:
				return row
		time.sleep(1)
	raise AssertionError(f'memo {title!r} {"still listed" if gone else "not listed"}, '
	                     f'list has {memo_titles()}')


def player_of(row, nodes=None):
	"""The play button of the open card, if the card belongs to row.

	The card hangs under its own row, so the row right above the play button owns it.
	"""
	nodes = dump() if nodes is None else nodes
	rows = find_all(nodes, rid='memo_row')
	for play in find_all(nodes, rid='player_play'):
		above = [r for r in rows if r.y2 <= play.y1]
		if above and max(above, key=lambda r: r.y2).y1 == row.y1:
			return play
	return None


def expand(title):
	"""Open the inline player of this memo, closing another one that is open."""
	for _ in range(3):
		row = wait_memo(title)
		if player_of(row) is not None:
			return
		tap_node(row)
		time.sleep(1)
		row = wait_memo(title)
		if player_of(row) is not None:
			return
	raise AssertionError(f'the player of {title!r} never opened')


def title_of(display_name):
	return os.path.splitext(display_name)[0]


def start_recording():
	tap(rid='record_button')
	allow_system_dialog(timeout=2)
	wait_for(text='New Recording')
	wait_for(desc='Pause', timeout=20)


def open_more(title):
	expand(title)
	tap(rid='card_more')
	wait_for(text='Rename')


# ---------------------------------------------------------------------------------------------
# steps


def step_start():
	for attempt in range(3):
		launch()
		try:
			wait_for(rid='record_button', timeout=20)
			break
		except AssertionError:
			if attempt == 2:
				raise
	wait_memo(SEED_TITLE, timeout=20)


def step_record_pause_resume_save():
	before = recordings()
	start_recording()
	time.sleep(4)
	tap(desc='Pause')
	wait_for(text='Paused')
	shot('recorder-paused')
	time.sleep(2)
	tap(desc='Resume')
	wait_for(desc='Pause')
	time.sleep(3)
	tap(text='Done')
	wait_for(text='New Recording', gone=True)
	name = wait_new_recording(before)
	STATE['recorded'] = title_of(name)
	wait_memo(STATE['recorded'])


def step_record_discard():
	before = recordings()
	start_recording()
	time.sleep(2)
	tap(text='Cancel')
	wait_for(text='Discard Recording?')
	shot('discard-alert')
	tap_in_dialog('Discard Recording?', 'Discard')
	wait_for(text='New Recording', gone=True)
	time.sleep(3)
	after = recordings()
	extra = set(after) - set(before)
	assert not extra, f'the discarded recording was kept: {extra}'


def step_record_in_background():
	before = recordings()
	start_recording()
	back()
	wait_for(rid='recording_indicator')
	shot('recording-indicator')
	home()
	time.sleep(4)
	launch()
	wait_for(rid='recording_indicator', timeout=20)
	# the record button reads as stop while a recording runs and saves it straight away
	tap(rid='record_button', desc='Stop')
	wait_for(rid='recording_indicator', gone=True)
	wait_new_recording(before)


def step_notification_buttons():
	before = recordings()
	start_recording()
	back()
	# android 12 and up hold a foreground service notification back for up to ten seconds
	time.sleep(11)
	sh('cmd statusbar expand-notifications')
	time.sleep(2)
	deadline = time.time() + 30
	while find(rid='notification_text', contains='running') is None:
		# android 13 and up group the app's notifications and collapse the group
		for button in find_all(desc='Expand', package='com.android.systemui'):
			tap_node(button, pause=1)
		if time.time() > deadline:
			raise AssertionError('the recorder notification never showed up')
		time.sleep(2)
	shot('notification-running')
	# the buttons are images, they carry the app's ids and descriptions
	tap(rid='pause_button', timeout=10)
	wait_for(rid='notification_text', contains='paused', timeout=15)
	shot('notification-paused')
	tap(rid='resume_button', timeout=10)
	wait_for(rid='notification_text', contains='running', timeout=15)
	tap(rid='stop_button', timeout=10)
	sh('cmd statusbar collapse')
	time.sleep(2)
	launch()
	wait_new_recording(before)
	wait_for(rid='recording_indicator', gone=True)


def step_play_seek_speed():
	# like iOS a memo starts playing as soon as it is opened
	expand(STATE['recorded'])
	timeline = []
	for _ in range(8):
		nodes = dump()
		play = find(nodes, rid='player_play')
		clocks = [n.text for n in nodes if re.fullmatch(r'-?\d+:\d\d', n.text)
		          and play is not None and abs(n.center[1] - play.center[1]) < 260]
		timeline.append(f'{play.desc if play else "?"} {"/".join(clocks)}')
		if play is not None and play.desc.casefold() == 'pause':
			break
		time.sleep(1)
	print('  player after opening: ' + ', '.join(timeline), flush=True)
	assert any(t.lower().startswith('pause') for t in timeline), \
		f'opening the memo did not start it: {timeline}'
	shot('playing')
	# pause and play again by hand
	tap(rid='player_play')
	wait_for(rid='player_play', desc='Play')
	tap(rid='player_play')
	wait_for(rid='player_play', desc='Pause')
	tap(rid='player_skip_forward')
	tap(rid='player_skip_back')
	def speed_label():
		nodes = dump()
		button = find(nodes, rid='player_speed')
		labels = [n.text for n in nodes if '×' in n.text and button is not None
		          and abs(n.center[0] - button.center[0]) < 80 and abs(n.center[1] - button.center[1]) < 80]
		return labels[0] if labels else '?'

	speeds = [speed_label()]
	for _ in range(4):
		tap(rid='player_speed')
		speeds.append(speed_label())
	assert len(set(speeds)) == 4, f'the speed button went through {speeds}'
	bar = wait_for(rid='player_waveform')
	sh(f'input tap {bar.x1 + (bar.x2 - bar.x1) // 2} {bar.center[1]}')
	time.sleep(1)
	# drag the waveform as a scrub, to the middle, the end would finish the playback and a
	# tap meant to pause would start it over
	swipe(bar.x1 + (bar.x2 - bar.x1) * .1, bar.center[1], bar.x1 + (bar.x2 - bar.x1) * .5,
	      bar.center[1], ms=600)
	if find(rid='player_play', desc='Pause') is not None:
		tap(rid='player_play')
	wait_for(rid='player_play', desc='Play', timeout=30)


def step_rename():
	new_title = 'E2E renamed'
	open_more(STATE['recorded'])
	tap(text='Rename')
	field = wait_for(cls='android.widget.EditText')
	tap_node(field)
	clear_field()
	type_text(new_title)
	shot('rename-dialog')
	hide_keyboard()
	tap_in_dialog('Name', 'Save')
	wait_memo(new_title)
	STATE['recorded'] = new_title


def step_favourite():
	open_more(STATE['recorded'])
	tap_lowest('Favourite')
	time.sleep(1)
	open_more(STATE['recorded'])
	wait_for(text='Remove Favourite')
	shot('favourite')
	tap_lowest('Remove Favourite')
	open_more(STATE['recorded'])
	wait_for(text='Favourite')
	tap_lowest('Cancel')


def step_search():
	tap(rid='memos_search')
	type_text('renamed')
	hide_keyboard()
	time.sleep(1)
	titles = memo_titles()
	shot('search')
	assert titles == [STATE['recorded']], f'searching for renamed listed {titles}'
	tap(rid='memos_search')
	clear_field()
	hide_keyboard()
	wait_memo(SEED_TITLE)


def step_share():
	expand(STATE['recorded'])
	tap(rid='card_share')
	time.sleep(3)
	others = {n.package for n in dump()} - {PKG, 'com.android.systemui'}
	assert others, 'no share sheet showed up'
	shot('share-sheet')
	back()
	time.sleep(1)
	go_home()


def step_trim_and_save():
	before = recordings()
	expand(STATE['recorded'])
	tap(rid='player_trim')
	wait_for(text='Edit Recording')
	tap(desc='Play')
	time.sleep(2)
	tap(desc='Pause')
	bar = wait_for(rid='trim_waveform')
	width = bar.x2 - bar.x1
	# the handles sit on the two ends, pull both inwards
	swipe(bar.x1 + width * .02, bar.center[1], bar.x1 + width * .3, bar.center[1], ms=600)
	swipe(bar.x2 - width * .02, bar.center[1], bar.x2 - width * .3, bar.center[1], ms=600)
	shot('trim-selection')
	tap(text='Trim')
	time.sleep(1)
	save = wait_for(text='Save')
	assert save.enabled, 'save stayed disabled after a trim'
	tap_node(save)
	deadline = time.time() + 60
	while time.time() < deadline and find(rid='trim_waveform') is not None:
		time.sleep(1)
	wait_new_recording(before, timeout=30)
	go_home()
	# the export is copied into media store and then has to go, run-as needs a debug build
	deadline = time.time() + 15
	left = None
	while time.time() < deadline:
		out = sh(f'run-as {PKG} ls cache/transformers').strip()
		if 'not debuggable' in out or 'unknown package' in out:
			return
		left = [f for f in out.split() if 'No such file' not in out]
		if not left:
			return
		time.sleep(1)
	raise AssertionError(f'the trim export was left in the cache: {left}')


def step_trim_delete_section_and_cancel():
	expand(STATE['recorded'])
	tap(rid='player_trim')
	wait_for(text='Edit Recording')
	bar = wait_for(rid='trim_waveform')
	width = bar.x2 - bar.x1
	swipe(bar.x1 + width * .02, bar.center[1], bar.x1 + width * .4, bar.center[1], ms=600)
	tap(text='Delete')
	shot('trim-delete-section')
	tap(text='Cancel')
	# the player card's trim button carries the same label, the editor's waveform does not
	wait_for(rid='trim_waveform', gone=True)
	wait_for(rid='record_button')


def delete_alert():
	tap_in_dialog('Delete Recording?', 'Delete')


def step_delete_from_player():
	# the trimmed copy is the newest one that is not the renamed recording
	names = [title_of(n) for n in recordings()]
	victim = next(t for t in names if t not in (STATE['recorded'], SEED_TITLE))
	expand(victim)
	tap(rid='card_delete')
	delete_alert()
	allow_system_dialog(timeout=3)
	wait_memo(victim, gone=True)
	STATE['deleted'] = victim


def step_delete_by_swipe():
	names = [title_of(n) for n in recordings()]
	victim = next(t for t in names if t not in (STATE['recorded'], SEED_TITLE))
	row = wait_memo(victim)
	w, _ = screen_size()
	swipe(w * .85, row.center[1], w * .1, row.center[1], ms=400)
	shot('swipe-actions')
	tap(rid='swipe_delete')
	delete_alert()
	allow_system_dialog(timeout=3)
	wait_memo(victim, gone=True)


def step_change_one_left_behind():
	"""A recording this install did not make, the system asks the user before every change.

	Android 11 and up show a write or trash request, android 10 the dialog of the security
	exception the change ran into. The recording is renamed and then deleted.
	"""
	renamed = SEED_TITLE + ' kept'
	open_more(SEED_TITLE)
	tap_lowest('Rename')
	field = wait_for(cls='android.widget.EditText')
	tap_node(field)
	clear_field()
	type_text(renamed)
	hide_keyboard()
	tap_in_dialog('Name', 'Save')
	shot('system-write-approval')
	assert allow_system_dialog(timeout=10), 'the system never asked to approve the rename'
	wait_memo(renamed, timeout=20)

	row = wait_memo(renamed)
	w, _ = screen_size()
	swipe(w * .85, row.center[1], w * .1, row.center[1], ms=400)
	tap(rid='swipe_delete')
	delete_alert()
	shot('system-trash-approval')
	asked = allow_system_dialog(timeout=10)
	# android 10 keeps the write access the rename was given, it need not ask again
	assert asked or API < 30, 'the system never asked to approve the delete'
	wait_memo(renamed, gone=True)


def step_recently_deleted():
	tap(desc='Recently Deleted')
	wait_for(text='Delete All')
	shot('recently-deleted')
	rows = find_all(contains='Expires')
	assert rows, 'the deleted recordings are not listed'

	# recover the first one
	tap_node(rows[0])
	tap_lowest('Recover')
	allow_system_dialog(timeout=3)
	time.sleep(2)

	# purge the next one, if an earlier step left more than one
	if find(contains='Expires') is not None:
		tap(contains='Expires')
		tap_lowest('Delete Permanently')
		allow_system_dialog(timeout=3)
		time.sleep(2)

	# and the rest in one go
	if find(text='Delete All') is not None:
		tap(text='Delete All')
		tap_in_dialog('Delete All Recordings?', 'Delete All')
		allow_system_dialog(timeout=3)
	wait_for(text='No Deleted Recordings', timeout=20)
	tap(text='Back')
	wait_for(rid='record_button')


def open_settings():
	tap(desc='Settings')
	wait_for(text='Appearance')


def settings_back():
	tap(text='Back')
	wait_for(rid='record_button')


def scroll_to(text, tries=6):
	w, h = screen_size()
	for _ in range(tries):
		node = find(text=text)
		if node is not None and node.y2 < h * .92:
			return node
		swipe(w / 2, h * .7, w / 2, h * .35)
	return wait_for(text=text)


def step_settings_appearance():
	open_settings()
	for mode in ('Dark', 'Light', 'Automatic'):
		tap(text=mode)
		time.sleep(1.5)
		node = wait_for(text=mode)
		assert node.selected or find(desc=mode) is None, f'{mode} is not shown as selected'
		shot(f'theme-{mode.lower()}')
	settings_back()


def step_settings_switches():
	open_settings()
	for tag in ('switch_stereo', 'switch_skip_silence', 'switch_pause_on_call',
	            'switch_add_location', 'switch_bluetooth_mic'):
		scroll_to('Use Bluetooth Microphone' if tag == 'switch_bluetooth_mic' else 'Format')
		node = wait_for(rid=tag)
		before = node.checked
		tap_node(node)
		allow_system_dialog(timeout=2)
		after = wait_for(rid=tag).checked
		assert after != before, f'{tag} did not change'
		tap(rid=tag)
		allow_system_dialog(timeout=2)
		assert wait_for(rid=tag).checked == before, f'{tag} did not change back'
	shot('settings-switches')
	settings_back()


def step_settings_file_name():
	open_settings()
	tap(text=scroll_to('Recording Name').text)
	field = wait_for(cls='android.widget.EditText')
	tap_node(field)
	clear_field()
	type_text('E2E')
	hide_keyboard()
	tap_in_dialog('Name', 'Save')
	wait_for(text='E2E')
	shot('settings-file-name')
	settings_back()
	before = recordings()
	start_recording()
	time.sleep(3)
	tap(text='Done')
	name = wait_new_recording(before)
	assert name.startswith('E2E'), f'the recording is called {name}'


def set_format(label, quality=None, stereo=None):
	open_settings()
	if quality is not None:
		tap(text=quality)
	if stereo is not None:
		node = wait_for(rid='switch_stereo')
		if node.checked != stereo:
			tap_node(node)
	tap(text=scroll_to('Format').text)
	# the sheet lists every format, the row behind it shows the current one too
	tap_lowest(label)
	time.sleep(1)
	wait_for(text=label)
	settings_back()


FORMATS = [
	('AAC', None, None),
	('Opus', None, None),
	('AMR narrow band', None, None),
	('AMR wide band', None, None),
	('AAC', 'Lossless', True),
	('AAC', 'Compressed', False),
	('AAC', 'Standard', False),
]


def step_every_format():
	problems = []
	for label, quality, stereo in FORMATS:
		what = label + (f', {quality}' if quality else '') + (', stereo' if stereo else '')
		try:
			if label == 'Opus' and API < 30:
				# android 10 has no media recorder encoder for it, it must not be offered
				open_settings()
				tap(text=scroll_to('Format').text)
				wait_for(text='AAC')
				shot('format-sheet-without-opus')
				offered = find(text='Opus') is not None
				tap_lowest('Cancel')
				settings_back()
				assert not offered, 'Opus is offered on android 10'
				print(f'  {what}: not offered on android 10 as expected', flush=True)
				continue
			set_format(label, quality, stereo)
			before = recordings()
			start_recording()
			time.sleep(3)
			tap(text='Done')
			name = wait_new_recording(before)
			print(f'  {what}: {name}', flush=True)
			shot('format-' + re.sub(r'\W+', '-', what.lower()).strip('-'))
		except Exception as e:
			problems.append(f'{what}: {e}')
			check_crash(f'format {what}')
			go_home()
	set_format('AAC', 'Standard', False)
	assert not problems, '; '.join(problems)


def rotate(quarter):
	sh('settings put system accelerometer_rotation 0')
	sh(f'settings put system user_rotation {quarter}')
	time.sleep(4)
	# the activity is recreated, wait for the app to be drawn again
	deadline = time.time() + 15
	while time.time() < deadline and find(package=PKG) is None:
		time.sleep(1)


def step_rotation():
	try:
		before = recordings()
		rotate(1)
		wait_for(rid='record_button')
		shot('landscape-memos')
		open_settings()
		shot('landscape-settings')
		settings_back()
		rotate(0)
		# the recording has to live through the activity being recreated, twice
		start_recording()
		rotate(1)
		shot('landscape-recorder')
		rotate(0)
		wait_for(text='New Recording')
		tap(text='Done')
		wait_for(text='New Recording', gone=True)
		wait_new_recording(before)
	finally:
		rotate(0)
	wait_for(rid='record_button')


def step_system_dark_mode():
	try:
		sh('cmd uimode night yes')
		time.sleep(3)
		wait_for(rid='record_button')
		shot('system-dark')
	finally:
		sh('cmd uimode night no')
		time.sleep(2)
	wait_for(rid='record_button')


def step_killed_in_background():
	home()
	time.sleep(2)
	sh(f'am kill {PKG}')
	time.sleep(2)
	launch()
	wait_for(rid='record_button', timeout=30)
	wait_memo(STATE['recorded'])


def step_deep_links():
	sh(f'am start -W -a android.intent.action.VIEW -d app://voicememos/recordings {PKG}')
	wait_for(rid='record_button')
	audio_id = max(i for i, _ in recordings().values())
	sh(f'am start -W -a android.intent.action.VIEW -d app://voicememos/player/{audio_id} {PKG}')
	wait_for(rid='player_play', timeout=20)
	shot('deep-link-player')
	sh(f'am start -W -a android.intent.action.VIEW -d app://voicememos/player/999999 {PKG}')
	wait_for(rid='record_button')
	sh(f'am start -W -a android.intent.action.VIEW -d app://voicememos {PKG}')
	wait_for(rid='record_button')


def step_microphone_refused():
	sh(f'pm revoke {PKG} android.permission.RECORD_AUDIO')
	# refused twice it counts as don't ask again from android 11, a retry could not ask at all
	sh(f'pm clear-permission-flags {PKG} android.permission.RECORD_AUDIO user-set user-fixed')
	time.sleep(1)
	launch()
	tap(rid='record_button', timeout=30)
	shot('microphone-permission')
	assert deny_system_dialog(), 'the microphone permission was never asked for'
	time.sleep(2)
	wait_for(rid='record_button')
	assert find(text='New Recording') is None, 'the recorder opened without a microphone'
	# allowed the second time round
	tap(rid='record_button')
	if allow_system_dialog():
		tap(rid='record_button')
	wait_for(text='New Recording', timeout=20)
	# the permission screen just closed, a tap at once can go nowhere while the window gets
	# its focus back, so give it a moment and tap again if the question did not come
	time.sleep(2)
	for _ in range(3):
		tap(text='Cancel')
		if find(text='Discard Recording?') is not None:
			break
		time.sleep(1)
	tap_in_dialog('Discard Recording?', 'Discard')


def step_media_access_refused():
	media = (['android.permission.READ_MEDIA_AUDIO'] if API >= 33 else
	         # one group below android 13, revoking only read leaves the group granted
	         ['android.permission.READ_EXTERNAL_STORAGE', 'android.permission.WRITE_EXTERNAL_STORAGE'])
	for permission in media:
		sh(f'pm revoke {PKG} {permission}')
	time.sleep(1)
	launch()
	wait_for(text='Allow Access', timeout=30)
	shot('media-permission-box')
	tap(text='Allow Access')
	assert allow_system_dialog(), 'the audio permission was never asked for'
	wait_memo(STATE['recorded'], timeout=20)


STEPS = [
	step_start,
	step_record_pause_resume_save,
	step_record_discard,
	step_record_in_background,
	step_notification_buttons,
	step_play_seek_speed,
	step_rename,
	step_favourite,
	step_search,
	step_share,
	step_trim_and_save,
	step_trim_delete_section_and_cancel,
	step_delete_from_player,
	step_delete_by_swipe,
	step_change_one_left_behind,
	step_recently_deleted,
	step_settings_appearance,
	step_settings_switches,
	step_settings_file_name,
	step_every_format,
	step_rotation,
	step_system_dark_mode,
	step_killed_in_background,
	step_deep_links,
	step_microphone_refused,
	step_media_access_refused,
]

# the steps after these need what they made, a failure there skips the dependants
NEEDS = {
	'recorded': {'step_play_seek_speed', 'step_rename', 'step_favourite', 'step_search',
	             'step_share', 'step_trim_and_save', 'step_trim_delete_section_and_cancel',
	             'step_delete_from_player', 'step_delete_by_swipe', 'step_killed_in_background',
	             'step_deep_links', 'step_media_access_refused'},
}

# ---------------------------------------------------------------------------------------------
# running


def shot(name):
	STATE['shots'] += 1
	path = os.path.join(OUT, f'{STATE["shots"]:03d}-{name}.png')
	with open(path, 'wb') as f:
		f.write(subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True).stdout)


def describe(nodes):
	"""The labelled nodes on screen, one line each, for the log of a failed step."""
	lines = ['  on screen:']
	for n in nodes:
		if n.text or n.desc or (n.rid and not n.rid.startswith('android:')):
			pkg = '' if n.package == PKG else f' [{n.package}]'
			lines.append(f'    w{n.window} {n.cls.split(".")[-1]} text={n.text!r} desc={n.desc!r} '
			             f'id={n.rid!r} at {n.center}{pkg}')
	return '\n'.join(lines[:80])


def app_log(lines=30):
	"""The app's latest warnings and errors, what it said while the step failed."""
	pid = sh(f'pidof {PKG}').strip().split()
	if not pid:
		return '  app log: the app is not running'
	out = adb('logcat', '-d', '-t', '2000', f'--pid={pid[0]}', '*:W').splitlines()
	return '  app log:\n' + '\n'.join('    ' + l for l in out[-lines:])


def check_crash(where):
	crash = adb('logcat', '-d', '-b', 'crash')
	if f'Process: {PKG}' in crash:
		with open(os.path.join(OUT, f'crash-{STATE["shots"]:03d}.txt'), 'w') as f:
			f.write(f'after {where}\n\n{crash}')
		adb('logcat', '-b', 'crash', '-c')
		lines = crash.splitlines()
		start = next((i for i, l in enumerate(lines) if f'Process: {PKG}' in l), 0)
		raise AssertionError('crashed:\n' + '\n'.join(lines[max(0, start - 1):start + 12]))
	if find(contains="isn't responding") is not None or find(contains='keeps stopping') is not None:
		tap(contains='Close app', timeout=3) if find(contains='Close app') else back()
		raise AssertionError('the system showed an app not responding or keeps stopping dialog')


def seed_recording():
	"""A second of tone in the app folder, made by the shell, so not owned by the app."""
	# what the previous APK left, media store keeps the rows of an uninstalled app's files
	# and android 10 then refuses those names to the next install
	where = f"relative_path LIKE '{RELATIVE}%'"
	sh('content delete --uri content://media/external/audio/media '
	   f'--where {shlex.quote(where)}')
	sh(f'rm -rf {FOLDER}')
	sh(f'mkdir -p {FOLDER}')
	with tempfile.TemporaryDirectory() as tmp:
		path = os.path.join(tmp, 'seed.wav')
		with wave.open(path, 'wb') as w:
			w.setnchannels(1)
			w.setsampwidth(2)
			w.setframerate(16000)
			w.writeframes(b''.join(struct.pack('<h', int(8000 * math.sin(2 * math.pi * 440 * i / 16000)))
			                       for i in range(16000)))
		adb('push', path, f'{FOLDER}/{SEED_TITLE}.wav', check=True)
	# android 10 still takes the scanner broadcast, later versions scan the volume on request
	sh(f'am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE '
	   f'-d {shlex.quote("file://" + FOLDER + "/" + SEED_TITLE.replace(" ", "%20") + ".wav")}')
	sh('content call --method scan_volume --uri content://media --arg external_primary')
	deadline = time.time() + 20
	while time.time() < deadline:
		if any(title_of(n) == SEED_TITLE for n in recordings()):
			return
		time.sleep(1)
	raise RuntimeError('media store never picked up the seeded recording')


def run_apk(apk):
	global PKG, OUT
	label = os.path.splitext(os.path.basename(apk))[0]
	PKG = package_of(apk)
	OUT = os.path.join(OUT_ROOT, label)
	os.makedirs(OUT, exist_ok=True)
	STATE.clear()
	STATE.update(shots=0)
	print(f'::group::{label} ({PKG}) on API {API}', flush=True)

	adb('uninstall', PKG)
	seed_recording()
	adb('install', '-r', '-g', apk, check=True, timeout=300)
	adb('logcat', '-b', 'all', '-c')

	results = []
	for step in STEPS:
		name = step.__name__
		missing = [k for k, users in NEEDS.items() if name in users and k not in STATE]
		if missing:
			results.append((name, 'skipped', f'needs {", ".join(missing)} from an earlier step', 0))
			print(f'- {name}: skipped', flush=True)
			continue
		started = time.time()
		first_failure = None
		# the device clock, to find the step in logcat
		print(f'▶ {name} at {sh("date +%H:%M:%S").strip()}', flush=True)
		# one more try for what the emulator gets wrong now and then, never for a crash
		for attempt in (1, 2):
			try:
				step()
				check_crash(name)
				shot(name.removeprefix('step_'))
				if first_failure is None:
					results.append((name, 'passed', '', time.time() - started))
					print(f'✓ {name} ({time.time() - started:.0f}s)', flush=True)
				else:
					results.append((name, 'flaky', f'passed on the second try, the first: {first_failure}',
					                time.time() - started))
					print(f'⚠ {name}: passed on the second try ({time.time() - started:.0f}s)', flush=True)
				break
			except Exception as e:
				message = str(e) or type(e).__name__
				if not isinstance(e, AssertionError):
					message = traceback.format_exc(limit=3)
				crashed = False
				try:
					check_crash(name)
				except AssertionError as crash:
					message = f'{message}\n{crash}'
					crashed = True
				shot(name.removeprefix('step_') + '-failed')
				nodes = dump()
				with open(os.path.join(OUT, f'{STATE["shots"]:03d}-{name}-failed-ui.xml'), 'w') as f:
					f.write(LAST_XML[0])
				print(describe(nodes), flush=True)
				print(app_log(), flush=True)
				try:
					go_home()
				except Exception as recover:
					print(f'  could not recover: {recover}', flush=True)
				if attempt == 1 and not crashed:
					first_failure = message
					print(f'  {name} failed, trying once more: {message}', flush=True)
					continue
				results.append((name, 'failed', message, time.time() - started))
				print(f'✗ {name}: {message}', flush=True)
				break

	with open(os.path.join(OUT, 'logcat.txt'), 'w') as f:
		f.write(adb('logcat', '-d', timeout=120))
	adb('uninstall', PKG)
	print('::endgroup::', flush=True)
	return label, results


def summarize(all_results):
	lines = [f'### Emulator test on API {API}', '']
	for label, results in all_results:
		passed = sum(1 for r in results if r[1] == 'passed')
		lines += [f'**{label}**: {passed} of {len(results)} steps passed', '',
		          '| step | result | time | detail |', '| --- | --- | --- | --- |']
		for name, result, detail, seconds in results:
			icon = {'passed': '✅', 'flaky': '⚠️', 'failed': '❌', 'skipped': '⏭️'}[result]
			detail = detail.replace('|', '\\|').replace('\n', '<br>')[:600]
			lines.append(f'| {name.removeprefix("step_")} | {icon} | {seconds:.0f}s | {detail} |')
		lines.append('')
	text = '\n'.join(lines)
	with open(os.path.join(OUT_ROOT, 'summary.md'), 'w') as f:
		f.write(text)
	if os.environ.get('GITHUB_STEP_SUMMARY'):
		with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as f:
			f.write(text + '\n')
	print(text, flush=True)


STATE = {}
PKG = OUT = ''

if __name__ == '__main__':
	os.makedirs(OUT_ROOT, exist_ok=True)
	apks = sorted(os.path.join(APK_DIR, f) for f in os.listdir(APK_DIR) if f.endswith('.apk'))
	if not apks:
		sys.exit(f'no APK in {APK_DIR}')
	print(f'emulator API level {API}, recordings in {RELATIVE}', flush=True)
	# keep the screen on and unlocked for the whole run
	sh('svc power stayon true')
	sh('wm dismiss-keyguard')
	# with gesture navigation a drag that starts near the edge is taken as back
	sh('cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton')
	time.sleep(2)
	all_results = [run_apk(apk) for apk in apks]
	summarize(all_results)
	failed = any(r[1] == 'failed' for _, results in all_results for r in results)
	sys.exit(1 if failed else 0)
