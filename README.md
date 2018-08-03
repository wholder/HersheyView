<p align="center"><img src="https://github.com/wholder/HersheyView/blob/master/images/HersheyView%20Screenshot.png"></p>

# HersheyView
This code is Java implementation of a program to render and display the [Hershey Vector fonts](https://en.wikipedia.org/wiki/Hershey_fonts) developed c. 1967 by Dr. Allen Vincent Hershey at the Naval Weapons Laboratory.  The code parses the fonts from a compact, text-based format beleived to have been developed by James Hurt at Cognition, Inc.

I originally created HersheyView as a tool to support the developoment of a set of vector fonts for my [LaserCut program](https://github.com/wholder/LaserCut) (still in development), but decided to publish it here to help draw attention to this pioneering work by Dr. Hershey.  His work formed the basis of modern, digital typography, but is today nearly forgotten.  For more information about Dr. Hershey's fonts, see the article "[Reviving the Hershey fonts](https://lwn.net/Articles/654819/)" by Adobe's Frank Grießhammer (from a presentation at [TypeCon](http://www.typecon.com/) 2015, in Denver CO)

###Features
 - Click the "`Show Origin`" checkbox to display the origin of the vectors that make up each character glyph.
 - Click the "`Show Grid`" checkbox to display a unit grid
 - Click the "`Show L/R`" checkbox to display red, vertical lines that show the horizontal space needed by the glyph.
 - Use the dropdown to select a zoom factor of 8 to 64 times.
 - Click the "`Line List`" button to display a list of all the lines (x1, y1, x2, y2) needed to draw the glyph.

Note: each glyph is assigned a code that's displayed in the upper left corner of the view area.  This code is unique to Dr. Hershey's fonts and does not correspond to standard ASCII, or any other character coding scheme.  However, the characters needed to build a set of ASCII characters is available in the set of glyphs.  In fact, multiple variations are included for single. double and stroke characters as well as italic versions.  There are also a wide variety of specialized symbnols included.   Dr. Hershey also developed a Japanese character font, but those glyphs are not included in the code.
### Requirements
Java 8 JDK, or later must be installed in order to compile the code.  There is also a [**Runnable JAR file**](https://github.com/wholder/HersheyView/tree/master/out/artifacts/HersheyView_jar) included in the checked in code that you can download.   On a Mac, just double click the `GerberPlot.jar` file and it should start.  Then, use `File->Open Gerber File` to open a file and then use the `Options` menu to select different render modes.  _Note: you may have to select the `GerberPlot.jar` file, then do a right click and select "Open" the first time you run the file to satisfy Mac OS' security checks._  You should also be able to run the JAR file on Windows or Linux systems, but you'll need to have a Java 8 JRE, or later installed and follow the appropriate process for each needed to run an executable JAR file.
