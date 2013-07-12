HoLOLcats
=========

Conversion of Google Lolcats sample app to Holo using the HoloAnywhere library.

This should run on any Android device that supports HoloAnywhere/ActionBarSherlock.

Changes:
- Updated the captioning dialog to use a DialogFragment instead of a Dialog, updating a deprecated method.
- Changed the ProgressDialog to a Toast and a progress indicator in the ActionBar, removing some deprecated methods.
- Changed the reporting dialog to launch another Activity with appropriate ActionBar.

Known bugs:
- The captioning dialog uses a lighter style for the buttons than the colors used for Holo Dark.
