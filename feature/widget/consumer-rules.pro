# Glance tells widgets apart by the class name of their GlanceAppWidget, updateAll on one
# looks up the receivers registered under that name. R8 merged the two widget classes
# into one, so updating the recorder widget also drew its content into the recordings
# widget, whose saved state is of another type, and that widget failed to load.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
