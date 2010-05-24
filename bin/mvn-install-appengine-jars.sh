export VERSION="1.3.4"
export SDK=$1
export LIB="${SDK}/lib"
export REPO=$2

echo Installing App Engine jars from ${LIB} to ${REPO}

mvn -npr install:install-file \
	-Dfile="${LIB}/user/appengine-api-1.0-sdk-${VERSION}.jar" \
	-DlocalRepositoryPath="${REPO}" \
	-DgroupId=com.google.appengine \
	-DartifactId=appengine-api-1.0-sdk \
	-Dversion=${VERSION} \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-DcreateChecksum=true

mvn install:install-file \
	-Dfile="${LIB}/impl/appengine-api-stubs.jar" \
	-DgroupId=com.google.appengine \
	-DartifactId=appengine-api-stubs \
	-Dversion=${VERSION} \
	-Dpackaging=jar \
	-DlocalRepositoryPath=${REPO} \
	-DgeneratePom=true \
	-DcreateChecksum=true

mvn install:install-file \
	-Dfile="${LIB}/impl/appengine-api.jar" \
	-DgroupId=com.google.appengine \
	-DartifactId=appengine-api \
	-Dversion=${VERSION} \
	-DlocalRepositoryPath=${REPO} \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-DcreateChecksum=true

mvn install:install-file \
	-Dfile="${LIB}/impl/appengine-api-labs.jar" \
	-DgroupId=com.google.appengine \
	-DartifactId=appengine-api-labs \
	-Dversion=${VERSION} \
	-DlocalRepositoryPath=${REPO} \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-DcreateChecksum=true

mvn install:install-file \
	-Dfile="${LIB}/testing/appengine-testing.jar" \
	-DgroupId=com.google.appengine \
	-DartifactId=appengine-testing \
	-DlocalRepositoryPath=${REPO} \
	-Dversion=${VERSION} \
	-Dpackaging=jar \
	-DgeneratePom=true \
	-DcreateChecksum=true
