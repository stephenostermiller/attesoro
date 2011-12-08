CLASSPATH=target/depend/classes
SOURCPATH=src/java
JAVACFLAGS=-classpath $(CLASSPATH) -sourcepath $(SOURCPATH) -Xlint:unchecked -d target/classes
JAVAC=javac $(JAVACFLAGS)
JLEXFLAGS=-q

.SUFFIXES:
.SUFFIXES: .java .class
.SUFFIXES: .bte .html

.PHONY: all
all: bundle

.PHONY: depend     
depend: target/depend

target/depend:
	./src/build/depend/download_ostermillerutils.sh

.PHONY: spell     
spell: target/spell/checked

target/spell/checked: src/java/com/Ostermiller/attesoro/*.java src/www/bte/*.bte
	./src/build/spell/spell.sh $?
        
.PHONY: www
www: website

.PHONY: web
web: website

.PHONY: site
site: website

.PHONY: website
website: bundle
	./src/build/www/website.sh
	
websiteclean:
	rm -f  target/www	

.PHONY: neaten
neaten:
	./src/build/srcformat/neaten.sh $?

.PHONY: compile 
compile: depend neaten spell
	./src/build/compile/javac.sh
        
.PHONY: clean	        
clean:
	rm -rf target/*.* target/classes target/jar target/neaten target/spell target/tgz target/www
	
.PHONY: allclean	        
allclean:
	rm -rf target

.PHONY: bundle
bundle: jar targz

.PHONY: jar
jar: compile
	./src/build/bundle/jar.sh

.PHONY: targz
targz:
	./src/build/bundle/targz.sh

.PHONY: install
install:
	@.src/build/install/install.sh

