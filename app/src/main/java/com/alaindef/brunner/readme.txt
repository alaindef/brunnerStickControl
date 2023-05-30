230519: to change the name:
    - strings.xml: <string name="app_name">adf brun.efd</string>
                    I use the first 3 letters of the last GIT sha
    - build.gradle: applicationId "com.alaindef.brunefd"
    - File - Sync Project with Gradle Files


        android:largeHeap="true"

230520: "Activity class {com.alaindef.brunefd/com.alaindef.brunner.Main} does not exist."
    -> SYNC gradle!

230530: I/Choreographer: Skipped 239 frames!  The application may be doing too much work on its main thread.
    I/OpenGLRenderer: Davey! duration=4008ms;
    -> timeout on receiver?? 4 sec. probeer sha b31a181: receiver in aparte thread