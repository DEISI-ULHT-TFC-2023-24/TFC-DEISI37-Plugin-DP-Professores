# Drop Project Teacher's plugin

<p align="center">⚠️ <strong>WARNING</strong> ⚠️</p>

<p align="center"><strong>THERE ARE STILL SOME CHANGES TO DP'S TEACHER API THAT HAVEN'T BEEN UPSTREAMED YET, SOME FEATURES MAY APPEAR BROKEN</strong></p>

## Broken features (on the official Drop Project build):
- Submission counter always displays 0, although it's still functional
- Viewing a student's last submission's build report or downloading it does not work

## Requirements:
- Access to a DP instance, preferably [self-hosted](https://github.com/joao-marques-a22108693/drop-project)
- IntelliJ IDEA 2022.2 or newer

## Installation:
1. Open the settings menu by pressing Ctrl+Alt+S
2. Go to the plugins section
3. Press the cog symbol at the top and select "Install Plugin from Disk..."
4. Select the .zip file containing the plugin

## Instructions:
There should now be a "DP" section on the menu bar, where you can login, list all the assignments visible to you, or open the dashboard.

To login, you'll need an API token, which you can get by going to <your instance's base URL>/personalToken.

To list a student's submissions from the assignments menu, you can press the number box next to the last submission's status.

**After downloading a submission, IntelliJ might not immediately display the project correctly.**<br>
If it doesn't load it correctly after a while, and no notification shows up regarding a maven project, then you'll have to reload the project.
