mvn -DlocalRepositoryPath=$1 -DcreateChecksum=true clean javadoc:jar source:jar install
