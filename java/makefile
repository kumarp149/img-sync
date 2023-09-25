SRCDIR := src

BINDIR := bin

EXTERNAL_CP := $(shell find lib -name '*.jar' -exec echo -n '{}:' \;)

EXTERNAL_JARS := $(shell find lib -name '*.jar')

FINAL_JAR := deploy.jar

SOURCES := $(shell find $(SRCDIR) -name '*.java')

CLASSES := $(patsubst $(SRCDIR)/%.java,$(BINDIR)/%.class,$(SOURCES))

JC := javac

JFLAGS := -d $(BINDIR) -source 17 -target 17


all: $(BINDIR)/$(FINAL_JAR)

$(BINDIR)/$(FINAL_JAR): $(CLASSES)
	@for jar in $(EXTERNAL_JARS); do \
	echo "extracting $$jar"; \
	unzip -n -d bin/ $$jar; \
	done
	jar cf bin/deploy.jar -C bin/ .


$(BINDIR)/%.class: $(SRCDIR)/%.java
	@mkdir -p $(@D)
	$(JC) $(JFLAGS) -cp $(EXTERNAL_CP) $(SOURCES)
	echo $(EXTERNAL_JARS)

$(BINDIR)/$(OUTPUT_JAR): $(CLASSES)
	jar cf $@ -C $(BINDIR) .

deploy:
	aws s3 cp bin/deploy.jar s3://sruteesh-jar-files/img-sync/source/
	aws lambda update-function-code --function-name IMG-SYNC --s3-bucket sruteesh-jar-files --s3-key img-sync/source/deploy.jar


clean:
	rm -rf bin/*




