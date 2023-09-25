build:
	$(MAKE) -C js/
	$(MAKE) -C java/

deploy:
	$(MAKE) -C js/ deploy
	$(MAKE) -C java/ deploy

clean:
	$(MAKE) -C js/ clean
	$(MAKE) -C java/ clean

