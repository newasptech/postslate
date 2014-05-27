dist_bin_SCRIPTS = postslate
dist_pkgdata_DATA = postslate.jar
pkgdatadir = $(datadir)/postslate/lib
noinst_SCRIPTS = \
	build.xml \
	postslate.bat
dist_jars = \
	lib/forms-1.3.0.jar \
	lib/jar-in-jar-loader.zip \
	lib/java-getopt-1.0.14.jar \
	lib/junit.jar \
	lib/org.hamcrest.core_1.1.0.v20090501071000.jar
dist_txt = \
	LICENSE.txt \
	README.txt
EXTRA_DIST = $(dist_jars) $(dist_txt) $(noinst_SCRIPTS) src
DISTCLEANFILES = postslate.jar
mfsrc=src/com/newasptech/postslate/gui/MainFrame.java
mftmp=$(mfsrc).tmp

postslate.jar:	build.xml
	sed -r "s;^(.*VERSION=\")([^\"]+);\1$(VERSION);" <$(mfsrc) >$(mftmp)
	mv -fv $(mfsrc) /tmp
	mv -fv $(mftmp) $(mfsrc)
	$(ANT) -f $<

clean-local:	build.xml
	$(ANT) -f $< clean
