name='rails-portlet'
jarlib=WEB-INF/lib
classes=WEB-INF/classes
version=`awk -F\" /String\ PORTLET_VERSION/{'print $$2'} WEB-INF/src/PortletVersion.java`
liferay_v=`grep 'LIFERAY_VERSION' WEB-INF/src/PortletVersion.java | grep -o .,. | tr ',' '.'`
pkgdir=..
hotdeploydir='/usr/local/liferay/tomcat/webapps/ROOT/WEB-INF/classes'

# version of Liferay's portal-service jar
#liferay=5.1.1
liferay=5.2.3

.PHONY: list deploy test help

all: compile list

clean:
	-find $(classes) -name *.class -exec rm -f {} \;
	if [ -e test/classes ]; then rm -rf test/classes/* ; fi
	if [ -e build ]; then rm -rf build; fi

compile: clean
	if [ ! -e $(classes) ]; then mkdir $(classes); fi
	#
	###################### compiling all Java classes
	#
# careful here, as the CLASSPATH easily falls out of the javac namespace,
# see the ';\' between the export and the for loop, it is required.
# portal-service and servlet-api are included to satisfy Liferay classes.
	
	export CLASSPATH="\
	$(jarlib)/portlet-2.0.jar:\
	$(jarlib)/commons-logging.jar:\
	$(jarlib)/commons-fileupload-1.2.1.jar:\
	$(jarlib)/commons-httpclient-3.1.jar:\
	$(jarlib)/portal-kernel.jar:\
	$(jarlib)/portal-service-$(liferay).jar:\
	$(jarlib)/servlet-api-2.4.jar:\
	$(jarlib)/htmlparser-1.6.jar" ;\
	javac WEB-INF/src/*.java -target jsr14 -Xlint:unchecked -Xlint:deprecation -d $(classes)

#xargs javac -target jsr14 -d classes/ <<< `find src/ -name *.java`

list:
	#
	###################### bytecode classes
	#
	tree $(classes)

test: compile
	#
	###################### running tests
	#
	if [ -e test/classes ]; then rm -rf test/classes/* ; fi
	mkdir -p test/classes
	export CLASSPATH="\
	$(classes):\
	test/classes:\
	$(jarlib)/portlet-2.0.jar:\
	$(jarlib)/commons-logging.jar:\
	$(jarlib)/commons-httpclient-3.1.jar:\
	$(jarlib)/commons-codec-1.3.jar:\
	$(jarlib)/commons-fileupload-1.2.1.jar:\
	$(jarlib)/portal-service-$(liferay).jar:\
	$(jarlib)/servlet-api-2.4.jar:\
	$(jarlib)/htmlparser-1.6.jar:\
	$(jarlib)/log4j-1.2.15.jar:\
	$(jarlib)/junit-4.6.jar:\
	$(jarlib)/spring-test-2.5.6.jar:\
	$(jarlib)/spring-core-2.5.6.jar:\
	$(jarlib)/spring-webmvc-2.5.6.jar:\
	$(jarlib)/spring-webmvc-portlet-2.5.6.jar:\
	test" ;\
	javac test/*.java -Xlint:unchecked -Xlint:deprecation -d test/classes && \
	time java -ea  org.junit.runner.JUnitCore com.celamanzi.liferay.portlets.rails286.TestLoader

hotdeploy: compile
	rm -rf $(hotdeploydir)/com/celamanzi/liferay/portlets/rails286/
	cp -r WEB-INF/classes/com $(hotdeploydir)

help:
	@echo "To compile classes:"
	@echo " 	make all"
	@echo
	@echo "To run the test suite:"
	@echo " 	make test (or ant test)"
	@echo
	@echo "To build the JAR"
	@echo " 	make jar"

prepare: clean
	#
	###################### preparing
	#
	mkdir build
	rsync * --exclude-from=.exclude -r build/
	cd build && make

jar: prepare
	#
	###################### creating JAR file
	#
	pkgdir=`pwd` && \
	version=$(version) && \
	pkg=$(name)-$$version.jar && \
	cd build/WEB-INF && \
	mv ../README classes && \
	cd classes && \
	echo $$version > VERSION && \
	jar cf "$$pkgdir/$$pkg" . && \
	echo " * done" && \
	cd $$pkgdir && ls -lh $$pkg && jar tf $$pkg


release: jar
	pkg=$(name)-$(version).jar && \
	mv $$pkg caterpillar/lib/java/
	#
	###################### JAR is located in caterpillar/lib/java/
	#
	ls -lh caterpillar/lib/java/



