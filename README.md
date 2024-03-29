HoLOLcats
=========

Conversion of Google Lolcats sample app to Holo using the HoloEverywhere library.

This should run on any Android device that supports HoloEverywhere/ActionBarSherlock.

To build, you **must** have working HoloEverywhere and ActionBarSherlock library projects in Eclipse. This hasn't been configured for Gradle or IntelliJ, but if you can get these two libraries working in these environments, the code and XML files should work as-is. Include these as library dependencies in the Android section of the project's settings.


Changes:
- Updated the captioning dialog to use a DialogFragment instead of a Dialog, updating a deprecated method.
- Changed the ProgressDialog to a Toast and a progress indicator in the ActionBar, removing some deprecated methods.
- Changed the reporting dialog to launch another Activity with appropriate ActionBar.

Known bugs:
- FIXED: ~~The captioning dialog uses a lighter style for the buttons than the colors used for Holo Dark.~~ The previous version used classes from the support library, not HoloEverywhere.
