import oracle.odi.domain.model.OdiReference

Collection<OdiReference> odiReferences = odiInstance.getTransactionalEntityManager()
				.findAll(OdiReference.class);
for (OdiReference odiReference : odiReferences) {
	if (odiReference == null) {
          println "odiReference  not valid : "			
	}
	if (odiReference.getPrimaryDataStore() == null) {
           println "odiReference not valid missing primary datastore for odireference : "+ odiReference.getName()					
	}
	if (odiReference.getPrimaryDataStore().getName() == null) {
          println "odiReference  not valid missing primary datastore name for odireference : "+ odiReference.getName()					
	}
}