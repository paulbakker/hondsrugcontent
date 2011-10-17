import groovy.io.FileType

/**
 * @Author Paul Bakker - paul.bakker.nl@gmail.com
 */
class RdfGenerator {
    private static final String CONTENT_PREFIX = "http%3A%2F%2Fluminis.net%2FDuits%23"
    private String inputDir
    private String outputDir

    RdfGenerator(String contentDir) {
        this.inputDir = contentDir + "/input"
        this.outputDir = contentDir + "/output"
    }

    public static void main(String[] args) {
        if(args.length < 1) {
            println("Usage: java -jar rdfgen.jar [/root/of/input]")
        } else {
            def generator = new RdfGenerator(args[0])
            generator.generateRdf()
        }
    }

    void generateRdf() {
        def outputDirf = new File(outputDir)
        outputDirf.deleteDir()
        outputDirf.mkdir()


        def header = """<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF xmlns:arl="http://www.luminis.net/ontologies/arl/1.1/arl.owl#" xmlns:contentfragment="http://www.luminis.net/ontologies/arl/1.1/contentfragment.owl#" xmlns:curriculum="http://www.luminis.net/ontologies/arl/1.1/curriculum.owl#" xmlns:learningasset="http://www.luminis.net/ontologies/arl/1.1/learningasset.owl#" xmlns:learningdesign="http://www.luminis.net/ontologies/arl/1.1/learningdesign.owl#" xmlns:learningobject="http://www.luminis.net/ontologies/arl/1.1/learningobject.owl#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:xsd="http://www.w3.org/2001/XMLSchema#" xmlns:dc="http://purl.org/dc/elements/1.1/" xml:base="http://luminis.net/Duits">
"""
        def learningAssets = """
    <learningasset:LearningAsset rdf:about="#Duits_MAVO3_periode_1">
		<rdfs:label>Duits MAVO3 - periode 1</rdfs:label>"""

        def learningDesigns = ""
        def learningActivities = ""
        def learningObjects = ""
        def fragments = ""

        File outputFile = new File("${outputDir}/index.rdf")

        def startDir = new File(inputDir)

        startDir.eachDir { dir ->
            learningAssets += generateLearningAsset(dir)
            learningDesigns += generateLearningDesign(dir)

            dir.eachDir { learningAssetDir ->

                learningActivities += generateLearningActivity(learningAssetDir)
                    learningObjects += generateLearningObject(learningAssetDir)
                    fragments += generateDiscreteFragment(learningAssetDir)


            }
        }

        learningAssets += """
    </learningasset:LearningAsset>"""
        outputFile.write(header + learningAssets + learningDesigns + learningActivities + learningObjects + fragments + "</rdf:RDF>")

    }

    private String generateLearningAsset(File dir) {
        return """
		<learningasset:hasLearningDesign rdf:resource="#${removeNumbering(dir)}"/>"""
    }

    private String generateLearningDesign(File dir) {
        def learningDesign = """
     <learningasset:LearningDesign rdf:about="#${removeNumbering(dir)}">
		<rdfs:label>${new File(dir.absolutePath + "/label.txt").text}</rdfs:label>"""

        dir.eachDir {
            learningDesign += """<learningdesign:hasActivity rdf:resource="#${removeNumbering(it).replace("-qti", "").replace("-html", "")}"/>"""
        }
        learningDesign += "</learningasset:LearningDesign>"

        return learningDesign
    }

    String generateLearningActivity(File dir) {
        def activityName = removeNumbering(dir).replace("-qti", "").replace("-html", "")
        def learningActivity = """
     <learningdesign:LearningActivity rdf:about="#${activityName}">
		<rdfs:label>${new File(dir.absolutePath + "/label.txt").text}</rdfs:label>
        <learningdesign:hasLearningObject rdf:resource="#obj-${activityName}"/>
    </learningdesign:LearningActivity>"""
        return learningActivity
    }

    private String generateLearningObject(File dir) {
        createLearningObjectDir(dir)

        def learningobject = """
    <learningobject:LearningObject rdf:about="#obj-${removeNumbering(dir).replace("-qti", "").replace("-html", "")}">
		<rdfs:label>${new File(dir.absolutePath + "/label.txt").text}</rdfs:label>
		<learningobject:hasContentFragment rdf:resource="#${removeNumbering(dir)}"/>
	</learningobject:LearningObject>"""

        return learningobject
    }

    void createLearningObjectDir(File dir) {
        def learningObjectDir = new File(outputDir + "/" + CONTENT_PREFIX + removeNumbering(dir))
        learningObjectDir.mkdir()
        new AntBuilder().copy(todir: learningObjectDir.absolutePath) {
            fileset(dir: dir.absolutePath)
        }
    }

    private String generateDiscreteFragment(File dir) {
        def val = null

        dir.eachFileMatch FileType.FILES, {it != "label.txt"}, { file ->
            println("Content fragment: ${file.name}")

            def contentFragment = """
    <contentfragment:DiscreteFragment rdf:about="#${removeNumbering(dir)}">
		<rdfs:label>no label</rdfs:label>
		<contentfragment:entrypoint>${file.name}</contentfragment:entrypoint>
	</contentfragment:DiscreteFragment>"""

            val = contentFragment
        }

        if (!val) {
            throw new RuntimeException("No files processed in ${dir.name}")
        }

        return val
    }

    private def removeNumbering(File dir) {
        dir.name.replaceAll(~/[0-9]{2}-/, "")
    }
}
