bin/deployment-package.zip: src/index.js makefile
	7z a -tzip bin/deployment-package.zip src/* package.json package-lock.json node_modules/*
build: bin/deployment-package.zip
deploy:
	aws lambda update-function-code --function-name getOAuthToken --zip-file fileb://bin/deployment-package.zip
test:
	aws lambda invoke --function-name getOAuthToken --payload fileb://bin/payload.json bin/output.json