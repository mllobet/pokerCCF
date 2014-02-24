INTRODUCTION

MultiConnect application is same like Simplechat application, Here in this application we can chat with mutiple users at a same time on a common window
by creating mutliple socket connection. Maximum connection supported by this application is 10.

Steps to complie and buid MultiConnect :

The following steps assume you already have installed Eclipse and the 
Android Platform SDK (http://developer.android.com/sdk/index.html) with 
API 15 or above.

1) 	In Eclipse, go to File -> New -> Project...
2) 	In the wizard, select Android -> Android Project from Existing Code.
3) 	Click Next.
4) 	Click Browse...
5) 	Select the folder that contains the MultiConnect project
	(ex. C:\SDK\Samples\MultiConnect).
6) 	Click Finish.  MultiConnect should display in Package Explorer
	
7) 	Open Properties of MultiConnect.
8) 	Go to Android and verify the "Project Build Target" is API Level 15 
	or greater.
9)	Close the properties window by clicking OK.
10) Go to File -> New -> Project...
11) In the wizard, select Android -> Android Project from Existing Code.
12) Click Next.
13) Click Browse...
14) Select the folder that contains the inproc_lib project (ex.
	C:\SDK\Samples\inproc_lib).
15) Click Finish.  inproc_lib should display in the Package Explorer as
	StarterActivity.  Rename this to inproc_lib if desired.
16) Open Properties of inproc_lib.
17) Go to Android and verify the "Project Build Target" is API Level 17
	or greater.  Also, make sure the checkbox "Is Library" is checked.
18) Click "Add External JARs...".  Browse and select the location of stclibcc.jar
	(ex. C:\SDK\Lib\stclibcc.jar).
19)	Next, Click "Add External JARs..." again.  Browse and select the
	location of android-support-v4.jar 
	(ex. C:\Program Files (x86)\Android\android-sdk\extras\android\support\v4).
	If this JAR is missing, open the Android SDK Manager and download it
	under Extras -> Android Support Library.
20) Go to the "Order and Export" tab.  Check stclibcc.jar and
	android-support-v4.jar.  Click OK.
21) Open Properties of MultiConnect Under the Android tab, in the library
	section, remove the broken link to inproc_lib and add the inproc_lib project
	from your environment.
22) Build inproc_lib.
23) Build MultiConnect

