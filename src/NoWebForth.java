// NOT is used here as a synonym for INVERT. Suggest replace by INVERT.
// Suggest replace EXPECT by ACCEPT
// Now that text drawing is faster, remove lines that defer text drawing

/*  NoWebForth for Java 1.0, Version 1.00
    Copyright (c) 1999 by Michael A. Losh  <mlosh@tir.com>,
    Philip Preston  <philip@preston20.freeserve.co.uk>, and
    Chris Jakeman  <cjakeman@bigfoot.com>.

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

    See the GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
    http://www.gnu.org/copyleft/gpl.html

    If you have made changes to the source code, please summarize
    them here:

    Date        Name            Change/Notes
    ---------------------------------------------------------------
    1998.12.27  Michael Losh    Alpha code,
                                version 0.90 (JANS-Forth)
    1999.01.30  Michael Losh    Integration of Preston's
                                ANS fixes: +LOOP, #, #S, #>, str, U.R, U., hi, NESTINPUT,
                                THRU, SM/REM.
                                Integration of Jakeman's faster console I/O.
    1999.02.05  Philip Preston  ENVIRONMENT? added

    1999.05.03  Chris Jakeman
        // Scrolling is flicker-free due to double-buffering
        // Focus is now indicated by a border in a contrasting colour.
        // Added Colours DARK-YELLOW, DARK-CYAN and DARK-MAGENTA
        // Renamed multi-word colors and coloured areas to use traditional "-"
        // Can set cursor colour from Forth. "OK" prompt also uses cursor colour.
        // DEF-BK-CLR changed to DEF-BG-CLR to match BG-CLR and FG-CLR.
        // PAGE now preserves FG-CLR and BG-CLR.
        // Default colors changed for a more modern look. Beware that some
        // colours give silly results on IE4 when Windows uses 256 colours.
        // Screen now is initialised before the (c) message appears.
        // update() re-defined to paint after the window is exposed.

    1999.06.04  Chris Jakeman
        // Added ANS KEY? as primitive in addition to eForth ?KEY
        // Arrow and function keys added to KEY

    1999.06.26  Philip Preston
        // Added word set for reading files
        // Refined 9th July when non-conformance identified on comp.lang.forth
        // "Line too long" error message corrected 16th July

    1999.06.29  Chris Jakeman
        // Added GET-XY to match AT-XY
        // and ROWS and COLS for applications to access

    1999.07.03  Philip Preston
        // To accelerate FIND, coded NAME-SEARCH as a primitive.
    1999.07.18  Chris Jakeman
        // Split doPrim() case statement into 2 parts to workaround
        // occasional problem with Sun's AppletViewer v1.02.
	2001.04.13  Chris Jakeman
        // Added primitive <! so that source can be kept in *.htm files
	2001.04.14  Chris Jakeman
        // Added primitives <B> and <I> so that HTML markup can be used in-line
        // Increased max line length for source files from 128 to 256
*/

// Note: Importing only the classes needed makes no significant
// difference to either compilation time nor executable size.
//import java.applet.Applet;
//import java.awt.Color;
//import java.awt.Event;
//import java.awt.Font;
//import java.awt.FontMetrics;
//import java.awt.Graphics;
//import java.io.IOException;
//import java.net.*;
import java.io.IOException;
import java.util.Scanner;

/////////////////////////////////////////////////////////////
// "NoWebForth" class manages VM and applet console I/O
/////////////////////////////////////////////////////////////
// This version (30-Jan-99) provides fast scrolling of text.
// Changes can be found by searching for the initials "CMJ".
//
// It takes advantage of the following situations which are
// very likely to be the case:
//   The cursor is likely to be near the end of the screen.
//   The background is likely to be the default colour.
//   Much of the text is spaces which need not be drawn.
//
// A single procedure paint_rest() is used to update the screen.
// - After scrolling, paint_rest() draws the whole screen.
// - At special points in the interpreter, paint_rest() is called
//   to draw from the first change to the end of the screen.
//
// These special points are:
//   whenever Forth is waiting for the user
//   before the Interpreter parses a line of input
//   whenever scroll() has moved the text.
//
// If this last entry is removed, then updates are even faster.
//
// An elegantly "winking" cursor is provided. This appears only
// when Forth is waiting for input from the user - as it should.

public class NoWebForth
{
    // Implementation Note:
    // Using 256 colours - beware that some colours are not reproduced correctly
    // by the Java engines in IE4 and Netscape 4.5 when Windows is set to 256 colours.
    // Eg: DECIMAL 40960 ( or HEX 00A000 ) CURSOR-CLR ! will result in
    // a brown, not a dark green on IE4 and HEX FFFFE0 appears white in Navigator
    // These problems do not arise on AppletViewer or with
    // Windows set to more than 256 colours.
    public int paramDEF_BG_CLR = 0x00FFFFE0; // default background color parameter: pale yellow
    public int paramFORTH_CLR = 0x00000000; // Color for default FORTH output: htmlBlack
    public int paramCURSOR_CLR = 0x00008000; // Color for cursor: dark green
    public int paramERROR_CLR = 0x00FF0000; // Color for FORTH error text output: htmlRed
    public int paramUSER_CLR = 0x00008080; // Color for echoing user keystrokes: htmlDarkTeal

    public int paramROWS = 18;
    public int paramCOLS = 70;
    public boolean refresh = true;
    private WebForthVM vm;

    // --Commented out by Inspection (3/14/2017 8:35 AM):private Image ScratchImage; //4 CMJ
    // --Commented out by Inspection (3/14/2017 8:35 AM):private Graphics gS;        //4 CMJ

    private NoWebForth ()
    {
    }

    public int cursor ()
    {
        return 0;
    }

    public void clear ()
    {
    }

    public void print (String s)
    {
        int i, len = s.length();
        for (i = 0; i < len; i++)
        {
            emitChar(s.charAt(i));
        }
    }

    public void emitChar (char c)
    {
        System.out.print(c);
    }    // Override default definition to ensure applet is redrawn whenever exposed

    public static void main (String[] args) throws IOException
    {
        NoWebForth wf = new NoWebForth();
        wf.vm = new WebForthVM(wf);
        wf.vm.prepare();
        wf.vm.start();

        Scanner in = new Scanner(System.in);
        for (;;)
        {
            String s = in.nextLine();
            for (int l=0; l<s.length(); l++)
            {
                wf.vm.enqueueKey(s.charAt(l));
            }
            wf.vm.enqueueKey('\n');
        }
    }
}

// Notes for the Manual
//
// Manual Note - Colours
// The Forth interpreter uses the following colours to display:
// ERROR-CLR  - show error messages
// USER-CLR   - echo user input
// CURSOR-CLR - blink cursor and prompt
// FORTH-CLR  - output text
// and uses the current foreground colour FG-CLR and background colour BG-CLR to do so.
//
// The initial value for FG-CLR is FORTH-CLR and for BG-CLR is DEF-BG-CLR
//
// The Forth user may change any of these values at any time.
// After QUIT is called (e.g. after an error), then BG-CLR is reset to DEF-BG-CLR.
// Once control is returned to the Interpreter, the current value of
// FG-CLR will be reset to FORTH-CLR.
//
// Initial values for all colours but FG-CLR and BG-CLR may be set by parameters
// in the Applet HTML file.

// Manual Note - Source Blocks
// Applets with many blocks of source in the HTML file:
// IE4 and Netscape Navigator behave differently here.
// Netscape re-loads the applet quickly and takes just 2-3 secs.

// IE4 re-loads the applet quickly but shows nothing (not even a blank applet window) until all
// the parameters in the HTML file have been scanned by IE.
// On my laptop, this takes 20 secs from clicking the link to "OK", whether 1 block is read
// or all 84.

// Manual Note - various
// WORDS takes 1.7 secs to scroll on IE4 (timed 10 cycles) using drawImage

// Beware that IE4 does not re-read the NoWebForth.class when
// View:Refresh is picked. The cached version is re-loaded instead.

// Manual Note - KEY
// KEY returns 1 to 255
// The Enter key is 10, Backspace is 8 and Tab is 9

// Cursor Positioning
// ROWS @ gives no. of characters down console
// COLS @ gives no. of characters across console
// GET-XY ( -- col row )
// ( col row -- ) AT-XY

