CLASSPATH=../../..
SOURCPATH=../../..
JFLAGS=-classpath $(CLASSPATH)
JDFLAGS=-classpath $(CLASSPATH) -sourcepath $(SOURCPATH)
JAVAC=javac $(JFLAGS)
JAVA=java $(JFLAGS)
JLEXFLAGS=-q
CVS=cvs -q

.SUFFIXES:
.SUFFIXES: .java .class
.SUFFIXES: .bte .html

all:
	@$(MAKE) -s --no-print-directory junkclean
	@$(MAKE) -s --no-print-directory spell
	@$(MAKE) -s --no-print-directory neaten
	@$(MAKE) -s --no-print-directory website
	@$(MAKE) -s --no-print-directory compile
	@$(MAKE) -s --no-print-directory build

spell: *.java *.bte
	@echo Make: Running spell check.
	@./spell.sh $?
	@touch spell

website: *.bte
	@echo Make: Compiling web documents.
	@bte $?
	@touch website
	
websiteclean:
	@echo Make: Removing web documents.
	@rm -f `find . -name "*.bte" | sed s/.bte/.html/`	

.PHONY : compile
compile: classes

neaten: *.java
	@./neaten.sh $?
	@touch neaten
	

JAVAFILES=$(wildcard *.java)
.PHONY: classes
classes: $(JAVAFILES:.java=.class)
	@# Write a bash script that will compile the files in the todo list
	@echo "#!/bin/bash" > tempCommand	
	@# If the todo list doesn't exist, don't compile anything
	@echo "if [ -e tempChangedJavaFileList ]" >> tempCommand
	@echo "then" >> tempCommand
	@# Make sure each file is only on the todo list once.
	@echo "sort tempChangedJavaFileList | uniq  > tempChangedJavaFileListUniq" >> tempCommand
	@echo "FILES=\`cat tempChangedJavaFileListUniq\`" >> tempCommand
	@# Compile the files.
	@echo "echo Make: Compiling: $$ FILES" >> tempCommand
	@echo "$(JAVAC) $$ FILES" >> tempCommand
	@echo "fi" >> tempCommand
	@# Remove extra spaces in the script that follow the dollar signs.
	@sed "s/\$$ /\$$/" tempCommand > tempCommand.sh
	@# Make the script executable.
	@chmod +x tempCommand.sh
	@# Call the script
	@./tempCommand.sh
	@rm -f tempCommand tempCommand.sh tempChangedJavaFileList tempChangedJavaFileListUniq

.java.class:
	@#for each changed java file, add it to the todo list.
	@echo "$<" >> tempChangedJavaFileList

.PHONY: classesclean
classesclean: junkclean
	@echo Make: Removing Java class files
	@rm -f *.class

.PHONY: junkclean	        
junkclean:
	@echo Make: Removing utilites detritus.
	@rm -rf *~ ~* temp* *.bak com/

.PHONY: buildclean	        
buildclean: junkclean
	@echo Make: Removing generated jar files.
	@rm -f attesoro.jar
        
.PHONY: clean	        
clean: buildclean websiteclean
	@echo Make: Removing generated class files.
	@rm -f *.class
	
.PHONY: allclean	        
allclean: clean
	@echo Make: Removing all built files.
	@rm -f neaten spell  website release

.PHONY: build
build: attesoro.jar

attesoro.jar: *.java *.bte *.png *.ini *.class *.sh *.properties *.dict Makefile 
	@echo Make: Building jar file.
	@mkdir -p com/Ostermiller/attesoro
	@cp *.java *.bte *.png *.ini *.class *.sh *.properties *.dict Makefile com/Ostermiller/attesoro/
	@mkdir -p com/Ostermiller/util
	@cp ../util/StringHelper* ../util/UberProperties* ../util/PropertiesLexer* ../util/PropertiesToken* ../util/Browser* com/Ostermiller/util/
	@jar mcfv Attesoro.mf attesoro.jar com/ > /dev/null
	@rm -rf com/

.PHONY: update
update: 
	@$(CVS) update -RPd .
        
.PHONY: commit
commit: 
	@$(CVS) commit

release: *.html *.css *.png attesoro.jar install.sh .htaccess
	@./release.sh $?
	@touch release

.PHONY: install
install:
	@./install.sh

