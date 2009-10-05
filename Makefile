# I don't know how to write ant tasks. This should be one. - lamikae

jarlib=WEB-INF/lib
classes=WEB-INF/classes

# version of Liferay's portal-service jar
#liferay=5.1.1
liferay=5.2.3

all: clean compile list

clean:
	find $(classes) -name *.class -exec rm {} -f \;
	rm test/classes/* -rf

compile:
	#
	###################### compiling all Java classes
	#
# careful here, as the CLASSPATH easily falls out of the javac namespace,
# see the ';\' between the export and the for loop, it is required.
# portal-service and servlet-api are included to satisfy Liferay classes.
	
# how to build packages that are compatible with older versions?
#$(jarlib)/portal-service-5.1.1.jar:\

	export CLASSPATH="\
	$(jarlib)/portlet-1.0.jar:\
	$(jarlib)/portlet-2.0.jar:\
	$(jarlib)/commons-logging.jar:\
	$(jarlib)/commons-httpclient-3.1.jar:\
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

test:
	#
	###################### running tests
	#
	if [ ! -e test/classes ]; then mkdir -p test/classes; fi
	echo $(classes)
	export CLASSPATH="\
	$(jarlib)/portlet-1.0.jar:\
	$(jarlib)/portlet-2.0.jar:\
	$(jarlib)/commons-logging.jar:\
	$(jarlib)/commons-httpclient-3.1.jar:\
	$(jarlib)/portal-service-$(liferay).jar:\
	$(jarlib)/servlet-api-2.4.jar:\
	$(jarlib)/htmlparser-1.6.jar:\
	$(jarlib)/log4j-1.2.15.jar:\
	$(jarlib)/junit-4.6.jar:\
	$(classes)" ;\
	javac test/*.java -Xlint:unchecked -Xlint:deprecation -d test/classes && \
	export CLASSPATH="$${CLASSPATH}:test/classes:test" && \
	time java -ea  org.junit.runner.JUnitCore com.youleaf.jsrproxy.test.TestLoader

# deploy:
#cp -r classes/com/celamanzi/liferay/portlets/rails286/*class /usr/local/liferay/webapps/ROOT/WEB-INF/classes/com/celamanzi/liferay/portlets/rails286/

.PHONY: list deploy test
