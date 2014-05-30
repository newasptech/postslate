AC_INIT(postslate,0.1.0)
AM_INIT_AUTOMAKE([foreign])
AC_CONFIG_SRCDIR([postslate])

AC_PROG_INSTALL
AC_PATH_PROG([ANT],[ant],[missing])
test $ANT = missing && AC_WARN([without ant, you will not be able to compile any changes to the source code using the provided scripts])

AC_PATH_PROG([FFMPEG],[ffmpeg],[missing])
AC_PATH_PROG([FFPLAY],[ffplay],[missing])
AC_PATH_PROG([FFPROBE],[ffprobe],[missing])
test $FFMPEG = missing || test $FFPLAY = missing || test $FFPROBE = missing && AC_ERROR([
Tools ffmpeg, ffplay and ffprobe from the FFMPEG package are required.
Please download and install ffmpeg, version 2 or higher, before running postslate.
See http://ffmpeg.org/download.html for details.])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
