package catena;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import catena.model.CandidateLinks;
import catena.model.classifier.EventDctTemporalClassifier;
import catena.model.classifier.EventEventTemporalClassifier;
import catena.model.classifier.EventTimexTemporalClassifier;
import catena.model.rule.EventDctTemporalRule;
import catena.model.rule.EventEventTemporalRule;
import catena.model.rule.EventTimexTemporalRule;
import catena.model.rule.TimexTimexTemporalRule;
import catena.parser.ColumnParser;
import catena.parser.TimeMLParser;
import catena.parser.TimeMLToColumns;
import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.EntityEnum;
import catena.model.feature.EventEventFeatureVector;
import catena.model.feature.EventTimexFeatureVector;
import catena.model.feature.PairFeatureVector;
import catena.model.feature.FeatureEnum.FeatureName;
import catena.model.feature.FeatureEnum.PairType;
import catena.parser.entities.TLINK;
import catena.parser.entities.TemporalRelation;
import catena.parser.entities.TimeMLDoc;
import catena.parser.entities.Timex;

public class Temporal {
	
	private boolean goldCandidate;
	private boolean ruleSieve;
	private boolean classifierSieve;
	private boolean reasoner;
	
	private boolean ttFeature;
	private boolean etFeature;
	
	private String edModelPath;
	private String etModelPath;
	private String eeModelPath;
	
	private String[] relTypes;
	
	public Temporal() {
		
	}

	public Doc filePreprocessing(File file, TimeMLToColumns tmlToCol, ColumnParser colParser,
			boolean columnFormat) throws Exception {
		return filePreprocessing(file, tmlToCol, colParser,
				null,
				columnFormat);
	}
	
	public Doc filePreprocessing(File file, TimeMLToColumns tmlToCol, ColumnParser colParser,
			Map<String, String> tlinks,
			boolean columnFormat) throws Exception {
		Doc doc;
		if (columnFormat) {
			doc = colParser.parseDocument(new File(file.getPath()), false);
		} else {
			System.err.println("Convert TimeML files to column format...");
			
			tmlToCol.convert(file, new File(file.getPath().replace(".tml", ".col")), true);
			doc = colParser.parseDocument(new File(file.getPath().replace(".tml", ".col")), false);
			
			// OR... Parse TimeML file without saving to .col files
//			List<String> columns = tmlToCol.convert(file, true);
//			doc = colParser.parseLines(columns);
		}				
		
		doc.setFilename(file.getName());
		if (columnFormat) {
			System.out.println(file.getAbsolutePath().replace("COL", "TML").replace(".\\","")
					.replace(".col", ".tml"));
			if (tlinks != null) TimeMLParser.setTlinks(tlinks, doc);

			else TimeMLParser.parseTimeML(new File(file.getAbsolutePath().replace("COL", "TML").replace(".\\","")
					.replace(".col", ".tml")), doc);
		} else {
			if (tlinks != null) TimeMLParser.parseTimeML(file, doc, tlinks, null);
			else TimeMLParser.parseTimeML(file, doc);
		}
		CandidateLinks.setCandidateTlinks(doc);
		
		return doc;
	}
	
	public void trainModels(String taskName, String dirpath, String[] labels, boolean columnFormat) throws Exception {
		trainModels(taskName, dirpath, null, labels, 
				new HashMap<String, String>(), columnFormat);
	}
	
	public void trainModels(String taskName, String dirpath, String[] labels, Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		trainModels(taskName, dirpath, null, labels, 
				relTypeMapping, columnFormat);
	}
	
	public void trainModels(String taskName, String dirPath, 
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels, Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		List<PairFeatureVector> edFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> etFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");	
		
		File[] files = new File(dirPath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
//			System.out.println(columnFormat);
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))) {
//				System.err.println("Processing " + file.getPath());
				
				// File pre-processing...
				Map<String, String> tlinks = null;
				if (tlinkPerFile != null) tlinks = tlinkPerFile.get(file.getName());
				System.out.println(tlinkPerFile.keySet());
				System.out.println(file.getName());
				System.out.println(tlinks);
				Doc	doc = filePreprocessing(file, tmlToCol, colParser, tlinks, columnFormat);
				
				Map<String, String> ttlinks = null, etlinks = null;		
				if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
				if (isETFeature()) etlinks = doc.getTlinkTypes();
				
				// Get the feature vectors
				edFvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
						true, true, Arrays.asList(labels), relTypeMapping));
				etFvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
						true, true, Arrays.asList(labels), relTypeMapping, ttlinks));
				eeFvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
						true, true, Arrays.asList(labels), relTypeMapping, etlinks));
			}
		}
		
		edCls.train(edFvList, getEDModelPath());
		etCls.train(etFvList, getETModelPath());
		eeCls.train(eeFvList, getEEModelPath());
	}
	
	public void trainModels(String taskName, String dirpath, String[] fileNames, 
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels, Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		List<PairFeatureVector> edFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> etFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");
		
		if (Arrays.asList(labels).contains("VAGUE")) {	//TimeBank-Dense --- Logistic Regression!
			edCls = new EventDctTemporalClassifier(taskName, "logit");
			etCls = new EventTimexTemporalClassifier(taskName, "logit");
			eeCls = new EventEventTemporalClassifier(taskName, "logit");
		} 
		
		List<String> fileList = Arrays.asList(fileNames);
		
		File[] files = new File(dirpath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((fileList.contains(file.getName())
					|| fileList.contains(file.getName().replace(".col", ".tml"))
					|| fileList.contains(file.getName().replace(".tml", ".col"))
				) &&
				((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml")))
				) {
//				System.err.println("Processing " + file.getPath());
				
				// File pre-processing...
				Map<String, String> tlinks = null;
				if (tlinkPerFile != null) tlinks = tlinkPerFile.get(file.getName());
				Doc	doc = filePreprocessing(file, tmlToCol, colParser, tlinks, columnFormat);	
				
				Map<String, String> ttlinks = null, etlinks = null;		
				if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
				if (isETFeature()) {
					etlinks = doc.getTlinkTypes();
					if (Arrays.asList(labels).contains("VAGUE"))	//TimeBank-Dense --- etlinks only from E-D rule labels 
						etlinks = EventDctTemporalRule.getEventDctRuleRelation(doc, true);
				}
				
				// Get the feature vectors
				edFvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
						true, true, Arrays.asList(labels), relTypeMapping));
				etFvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
						true, true, Arrays.asList(labels), relTypeMapping, ttlinks));
				eeFvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
						true, true, Arrays.asList(labels), relTypeMapping, etlinks));
			}
		}
		
		edCls.train(edFvList, getEDModelPath());
		etCls.train(etFvList, getETModelPath());
		eeCls.train(eeFvList, getEEModelPath());
	}
	
	public void printFeatures(String taskName, String dirPath, String[] labels,
			Map<String, String> relTypeMapping, 
			String outputDirPath, boolean train,
			boolean columnFormat) throws Exception {
		List<PairFeatureVector> edFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> etFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");	
		
		File[] files = new File(dirPath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))
					) {
//				System.err.println("Processing " + file.getPath());
				
				// File pre-processing...
				Doc doc = filePreprocessing(file, tmlToCol, colParser, columnFormat);
				
				Map<String, String> ttlinks = null, etlinks = null;		
				if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
				if (isETFeature()) etlinks = doc.getTlinkTypes();
				
				// Get the feature vectors
				edFvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
						true, true, Arrays.asList(labels), relTypeMapping));
				etFvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
						true, true, Arrays.asList(labels), relTypeMapping, ttlinks));
				eeFvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
						true, true, Arrays.asList(labels), relTypeMapping, etlinks));
			}
		}
		
		ensureDirectory(new File(outputDirPath));
		String trainOrEval = "eval";
		if (train) trainOrEval = "train";
		
		BufferedWriter bwED = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ed-"+trainOrEval+"-features.csv"));
		BufferedWriter bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-features.csv"));
		BufferedWriter bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-features.csv"));
		
		for (PairFeatureVector fv : edFvList) bwED.write(fv.vecToCSVString() + "\n");
		for (PairFeatureVector fv : etFvList) bwET.write(fv.vecToCSVString() + "\n");
		for (PairFeatureVector fv : eeFvList) bwEE.write(fv.vecToCSVString() + "\n");
		
		bwED.close();
		bwET.close();
		bwEE.close();
		
		bwED = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ed-"+trainOrEval+"-labels-str.csv"));
		bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-labels-str.csv"));
		bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-labels-str.csv"));
		
		for (PairFeatureVector fv : edFvList) bwED.write(fv.getLabel() + "\n");
		for (PairFeatureVector fv : etFvList) bwET.write(fv.getLabel() + "\n");
		for (PairFeatureVector fv : eeFvList) bwEE.write(fv.getLabel() + "\n");
		
		bwED.close();
		bwET.close();
		bwEE.close();
		
		bwED = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ed-"+trainOrEval+"-tokens.csv"));
		bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-tokens.csv"));
		bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-tokens.csv"));
		
		for (PairFeatureVector fv : edFvList) bwED.write("\"" + fv.getTokenAttribute(fv.getE1(), FeatureName.token) + "\""
				+ "," + "\"" + fv.getTokenAttribute(fv.getE2(), FeatureName.token) + "\""
				+ "\n");
		for (PairFeatureVector fv : etFvList) bwET.write("\"" + fv.getTokenAttribute(fv.getE1(), FeatureName.token) + "\""
				+ "," + "\"" + fv.getTokenAttribute(fv.getE2(), FeatureName.token) + "\""
				+ "\n");
		for (PairFeatureVector fv : eeFvList) bwEE.write("\"" + fv.getTokenAttribute(fv.getE1(), FeatureName.token) + "\""
				+ "," + "\"" + fv.getTokenAttribute(fv.getE2(), FeatureName.token) + "\""
				+ "\n");
		
		bwED.close();
		bwET.close();
		bwEE.close();
		
		bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-features-sent.csv"));
		bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-features-sent.csv"));
		
		for (PairFeatureVector fv : etFvList) {
			bwET.write((Integer.parseInt(fv.getE2().getSentID())-Integer.parseInt(fv.getE1().getSentID())) + "\n");
		}
		for (PairFeatureVector fv : eeFvList) {
			bwEE.write((Integer.parseInt(fv.getE2().getSentID())-Integer.parseInt(fv.getE1().getSentID())) + "\n");
		}
		
		bwET.close();
		bwEE.close();
	}
	
	public void printFeatures(String taskName, String dirpath, String[] fileNames, 
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels, String outputDirPath, boolean train,
			boolean columnFormat) throws Exception {
		List<PairFeatureVector> edFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> etFvList = new ArrayList<PairFeatureVector>();
		List<PairFeatureVector> eeFvList = new ArrayList<PairFeatureVector>();
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
		
		// Init the classifier...
		EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
		EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
		EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");
		
		if (Arrays.asList(labels).contains("VAGUE")) {	//TimeBank-Dense --- Logistic Regression!
			edCls = new EventDctTemporalClassifier(taskName, "logit");
			etCls = new EventTimexTemporalClassifier(taskName, "logit");
			eeCls = new EventEventTemporalClassifier(taskName, "logit");
		} 
		
		List<String> fileList = Arrays.asList(fileNames);
		
		File[] files = new File(dirpath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((fileList.contains(file.getName())
					|| fileList.contains(file.getName().replace(".col", ".tml"))
					|| fileList.contains(file.getName().replace(".tml", ".col"))
				) &&
				((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml")))
				) {
//				System.err.println("Processing " + file.getPath());
				
				// File pre-processing...
				Map<String, String> tlinks = tlinkPerFile.get(file.getName());
				Doc doc = filePreprocessing(file, tmlToCol, colParser, tlinks, columnFormat);
				
				Map<String, String> ttlinks = null, etlinks = null;		
				if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
				if (isETFeature()) {
					etlinks = doc.getTlinkTypes();
					if (Arrays.asList(labels).contains("VAGUE"))	//TimeBank-Dense --- etlinks only from E-D rule labels 
						etlinks = EventDctTemporalRule.getEventDctRuleRelation(doc, true);
				}
				
				// Get the feature vectors
				edFvList.addAll(EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
						true, true, Arrays.asList(labels)));
				etFvList.addAll(EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
						true, true, Arrays.asList(labels), ttlinks));
				eeFvList.addAll(EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
						true, true, Arrays.asList(labels), etlinks));
			}
		}
		
		ensureDirectory(new File(outputDirPath));
		String trainOrEval = "eval";
		if (train) trainOrEval = "train";
		
		BufferedWriter bwED = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ed-"+trainOrEval+"-features.csv"));
		BufferedWriter bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-features.csv"));
		BufferedWriter bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-features.csv"));
		
		for (PairFeatureVector fv : edFvList) bwED.write(fv.vecToCSVString() + "\n");
		for (PairFeatureVector fv : etFvList) bwET.write(fv.vecToCSVString() + "\n");
		for (PairFeatureVector fv : eeFvList) bwEE.write(fv.vecToCSVString() + "\n");
		
		bwED.close();
		bwET.close();
		bwEE.close();
		
		bwED = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ed-"+trainOrEval+"-labels-str.csv"));
		bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-labels-str.csv"));
		bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-labels-str.csv"));
		
		for (PairFeatureVector fv : edFvList) bwED.write(fv.getLabel() + "\n");
		for (PairFeatureVector fv : etFvList) bwET.write(fv.getLabel() + "\n");
		for (PairFeatureVector fv : eeFvList) bwEE.write(fv.getLabel() + "\n");
		
		bwED.close();
		bwET.close();
		bwEE.close();
		
		bwED = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ed-"+trainOrEval+"-tokens.csv"));
		bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-tokens.csv"));
		bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-tokens.csv"));
		
		for (PairFeatureVector fv : edFvList) bwED.write("\"" + fv.getTokenAttribute(fv.getE1(), FeatureName.token) + "\""
				+ "," + "\"" + fv.getTokenAttribute(fv.getE2(), FeatureName.token) + "\""
				+ "\n");
		for (PairFeatureVector fv : etFvList) bwET.write("\"" + fv.getTokenAttribute(fv.getE1(), FeatureName.token) + "\""
				+ "," + "\"" + fv.getTokenAttribute(fv.getE2(), FeatureName.token) + "\""
				+ "\n");
		for (PairFeatureVector fv : eeFvList) bwEE.write("\"" + fv.getTokenAttribute(fv.getE1(), FeatureName.token) + "\""
				+ "," + "\"" + fv.getTokenAttribute(fv.getE2(), FeatureName.token) + "\""
				+ "\n");
		
		bwED.close();
		bwET.close();
		bwEE.close();
		
		bwET = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-et-"+trainOrEval+"-features-sent.csv"));
		bwEE = new BufferedWriter(new FileWriter(outputDirPath+taskName+"-ee-"+trainOrEval+"-features-sent.csv"));
		
		for (PairFeatureVector fv : etFvList) {
			bwET.write((Integer.parseInt(fv.getE2().getSentID())-Integer.parseInt(fv.getE1().getSentID())) + "\n");
		}
		for (PairFeatureVector fv : eeFvList) {
			bwEE.write((Integer.parseInt(fv.getE2().getSentID())-Integer.parseInt(fv.getE1().getSentID())) + "\n");
		}
		
		bwET.close();
		bwEE.close();
	}
	
	public List<TLINK> extractRelations(String taskName, String dirpath, 
			String[] labels, Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		return extractRelations(taskName, dirpath, null, labels,
				relTypeMapping, columnFormat);
	}
	
	public List<TLINK> extractRelations(String taskName, String dirpath, 
			String[] labels, boolean columnFormat) throws Exception {
		return extractRelations(taskName, dirpath, null, labels,
				new HashMap<String, String>(), columnFormat);
	}
	
	public List<TLINK> extractRelations(String taskName, String dirpath, 
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels, boolean columnFormat) throws Exception {
		return extractRelations(taskName, dirpath, tlinkPerFile, labels,
				new HashMap<String, String>(), columnFormat);
	}
	
	public List<TLINK> extractRelations(String taskName, String dirpath, 
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels,
			Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		
		List<TLINK> results = new ArrayList<TLINK>();
		results.add(new TLINK());
		results.add(new TLINK());
		
		List<String> ttRules = new ArrayList<String>();
		List<String> edRules = new ArrayList<String>();
		List<String> etRules = new ArrayList<String>();
		List<String> eeRules = new ArrayList<String>();
		
		List<String> tt = new ArrayList<String>();
		List<String> ed = new ArrayList<String>();
		List<String> et = new ArrayList<String>();
		List<String> ee = new ArrayList<String>();
		
		File[] files = new File(dirpath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((columnFormat && file.getName().contains(".col"))
					|| (!columnFormat && file.getName().contains(".tml"))
					) {
				System.err.println("Processing " + file.getPath());
				
				// PREDICT
				Map<String, String> tlinks = null;
				if (tlinkPerFile != null) tlinks = tlinkPerFile.get(file.getName());
				List<TLINK> linksList = extractRelations(taskName, file, tlinks, labels, relTypeMapping, columnFormat);
				
				ttRules.addAll(linksList.get(0).getTT());
				edRules.addAll(linksList.get(0).getED());
				etRules.addAll(linksList.get(0).getET());
				eeRules.addAll(linksList.get(0).getEE());
				
				tt.addAll(linksList.get(1).getTT());
				ed.addAll(linksList.get(1).getED());
				et.addAll(linksList.get(1).getET());
				ee.addAll(linksList.get(1).getEE());
			}
		}
		
		results.get(0).setTT(ttRules);
		results.get(0).setED(edRules);
		results.get(0).setET(etRules);
		results.get(0).setEE(eeRules);
		
		results.get(1).setTT(tt);
		results.get(1).setED(ed);
		results.get(1).setET(et);
		results.get(1).setEE(ee);
		
		return results;
	}
	
	public List<TLINK> extractRelations(String taskName, File file, String[] labels, 
			Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// File pre-processing...
		Doc doc = filePreprocessing(file, tmlToCol, colParser, columnFormat);
		
		TimeMLDoc tmlDoc = null;
		if (columnFormat) {
			tmlDoc = doc.toTimeMLDoc(false, false);
		} else {
			tmlDoc = new TimeMLDoc(file);
			tmlDoc.removeLinks();
		}
		String tmlString = tmlDoc.toString();
		
		List<TLINK> resultList = new ArrayList<TLINK>();
					
		// Applying temporal rules...
		List<String> ttRule = TimexTimexTemporalRule.getTimexTimexTlinksPerFile(doc, this.isGoldCandidate());
		List<String> edRule = new ArrayList<String>();
		List<String> etRule = new ArrayList<String>();
		List<String> eeRule = new ArrayList<String>();
		
		if (isRuleSieve()) {			
			edRule = EventTimexTemporalRule.getEventDctTlinksPerFile(doc, this.isGoldCandidate());
			etRule = EventTimexTemporalRule.getEventTimexTlinksPerFile(doc, this.isGoldCandidate());
			eeRule = EventEventTemporalRule.getEventEventTlinksPerFile(doc, this.isGoldCandidate());
			
			if (columnFormat) {
				tmlDoc = doc.toTimeMLDoc(ttRule, edRule, etRule, eeRule);
				tmlString = tmlDoc.toString();
			} else { 
				tmlString = TimeMLDoc.timeMLFileToString(doc, file,
						ttRule, edRule, etRule, eeRule);
			}
		}
		
		//Applying temporal reasoner...
		if (isReasoner()) {			
			Reasoner r = new Reasoner();
			tmlString = r.deduceTlinksPerFile(tmlString);
		}
		
//		Doc docSieved = colParser.parseLines(columns);
		Doc docSieved = null;
		if (columnFormat) {
			docSieved = colParser.parseDocument(new File(file.getPath()), false);
		} else {
			docSieved = colParser.parseDocument(new File(file.getPath().replace(".tml", ".col")), false);
		}
		docSieved.setFilename(file.getName());
		TimeMLParser.parseTimeML(tmlString, docSieved.getFilename(), docSieved);	
		
		resultList.add(relationToString(doc, docSieved, relTypeMapping));
		
		//Applying temporal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
			EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
			EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");	
			
			//Init the feature vectors...	
			Map<String, String> ttlinks = null, etlinks = null;
			if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
			if (isETFeature()) etlinks = doc.getTlinkTypes();
			
			List<PairFeatureVector> edFv = EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
					false, isGoldCandidate(), Arrays.asList(labels));
			List<PairFeatureVector> etFv = EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
					false, isGoldCandidate(), Arrays.asList(labels), ttlinks);
			List<PairFeatureVector> eeFv = EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
					false, isGoldCandidate(), Arrays.asList(labels), etlinks);
			
			List<String> edClsLabels = edCls.predict(edFv, getEDModelPath(), labels);
			List<String> etClsLabels = etCls.predict(etFv, getETModelPath(), labels);
			List<String> eeClsLabels = eeCls.predict(eeFv, getEEModelPath(), labels);
			
			if (isRuleSieve()) {
				
				for (int i = 0; i < edFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < etFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(eefv.getE1().getID() + "," + eefv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(eefv.getE2().getID() + "," + eefv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
					}				
				}
				
			} else {
				for (int i = 0; i < edFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
				}
				for (int i = 0; i < etFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
				}
			}
		}
		
		// Temporal links to string
		resultList.add(relationToString(doc, docSieved, relTypeMapping));
		
		return resultList;
	}
	
	public List<TLINK> extractRelations(String taskName, String dirpath, String[] fileNames,
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels, boolean columnFormat) throws Exception {
		return extractRelations(taskName, dirpath, fileNames,
				tlinkPerFile,
				labels,
				new HashMap<String, String>(), columnFormat);
	}
	
	public List<TLINK> extractRelations(String taskName, String dirpath, String[] fileNames,
			Map<String, Map<String, String>> tlinkPerFile,
			String[] labels,
			Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		
		List<TLINK> results = new ArrayList<TLINK>();
		results.add(new TLINK());
		results.add(new TLINK());
		
		List<String> ttRules = new ArrayList<String>();
		List<String> edRules = new ArrayList<String>();
		List<String> etRules = new ArrayList<String>();
		List<String> eeRules = new ArrayList<String>();
		
		List<String> tt = new ArrayList<String>();
		List<String> ed = new ArrayList<String>();
		List<String> et = new ArrayList<String>();
		List<String> ee = new ArrayList<String>();
		
		List<String> fileList = Arrays.asList(fileNames);
		
		File[] files = new File(dirpath).listFiles();
		for (File file : files) {	//assuming that there is no sub-directory
			
			if ((fileList.contains(file.getName())
						|| fileList.contains(file.getName().replace(".col", ".tml"))
						|| fileList.contains(file.getName().replace(".tml", ".col"))
					) &&
					((columnFormat && file.getName().contains(".col"))
						|| (!columnFormat && file.getName().contains(".tml")))
					) {
				System.err.println("Processing " + file.getPath());
				
				// PREDICT
				Map<String, String> tlinks = tlinkPerFile.get(file.getName());
				List<TLINK> linksList = extractRelations(taskName, file, tlinks, labels, relTypeMapping, columnFormat);
				
				ttRules.addAll(linksList.get(0).getTT());
				edRules.addAll(linksList.get(0).getED());
				etRules.addAll(linksList.get(0).getET());
				eeRules.addAll(linksList.get(0).getEE());
				
				tt.addAll(linksList.get(1).getTT());
				ed.addAll(linksList.get(1).getED());
				et.addAll(linksList.get(1).getET());
				ee.addAll(linksList.get(1).getEE());
			}
		}
		
		results.get(0).setTT(ttRules);
		results.get(0).setED(edRules);
		results.get(0).setET(etRules);
		results.get(0).setEE(eeRules);
		
		results.get(1).setTT(tt);
		results.get(1).setED(ed);
		results.get(1).setET(et);
		results.get(1).setEE(ee);
		
		return results;
	}	
	
	public List<TLINK> extractRelations(String taskName, File file, Map<String, String> tlinks, 
			String[] labels,
			Map<String, String> relTypeMapping, boolean columnFormat) throws Exception {
		
		// Init the parsers...
		TimeMLToColumns tmlToCol = new TimeMLToColumns(ParserConfig.textProDirpath, 
				ParserConfig.mateLemmatizerModel, ParserConfig.mateTaggerModel, ParserConfig.mateParserModel);
		ColumnParser colParser = new ColumnParser(EntityEnum.Language.EN);
				
		// File pre-processing...
		Doc doc = filePreprocessing(file, tmlToCol, colParser, tlinks, columnFormat);
		
		TimeMLDoc tmlDoc = null;
		if (columnFormat) {
			tmlDoc = doc.toTimeMLDoc(false, false);
		} else {
			tmlDoc = new TimeMLDoc(file);
			tmlDoc.removeLinks();
		}
		String tmlString = tmlDoc.toString();
		
		List<TLINK> resultList = new ArrayList<TLINK>();
					
		// Applying temporal rules...
		List<String> ttRule = TimexTimexTemporalRule.getTimexTimexTlinksPerFile(doc, this.isGoldCandidate());
		List<String> edRule = new ArrayList<String>();
		List<String> etRule = new ArrayList<String>();
		List<String> eeRule = new ArrayList<String>();
		
		if (isRuleSieve()) {
			edRule = EventDctTemporalRule.getEventDctTlinksPerFile(doc, this.isGoldCandidate());
			etRule = EventTimexTemporalRule.getEventTimexTlinksPerFile(doc, this.isGoldCandidate());
			eeRule = EventEventTemporalRule.getEventEventTlinksPerFile(doc, this.isGoldCandidate());
			
			if (Arrays.asList(labels).contains("VAGUE")) {	//TimeBank-Dense --- special VAGUE rules for E-D and E-T
				edRule = EventDctTemporalRule.getEventDctTlinksPerFile(doc, this.isGoldCandidate(), true);
				etRule = EventTimexTemporalRule.getEventTimexTlinksPerFile(doc, this.isGoldCandidate(), true);
			}
			
			if (columnFormat) {
				tmlDoc = doc.toTimeMLDoc(ttRule, edRule, etRule, eeRule);
				tmlString = tmlDoc.toString();
			} else { 
				tmlString = TimeMLDoc.timeMLFileToString(doc, file,
						ttRule, edRule, etRule, eeRule);
			}
		}
		
		//Applying temporal reasoner...
		if (isReasoner()) {			
			Reasoner r = new Reasoner();
			tmlString = r.deduceTlinksPerFile(tmlString);
		}
		
//		Doc docSieved = colParser.parseLines(columns);
		Doc docSieved = null;
		if (columnFormat) {
			docSieved = colParser.parseDocument(new File(file.getPath()), false);
		} else {
			docSieved = colParser.parseDocument(new File(file.getPath().replace(".tml", ".col")), false);
		}
		docSieved.setFilename(file.getName());
		TimeMLParser.parseTimeML(tmlString, docSieved.getFilename(), docSieved);	
		
		resultList.add(relationToString(doc, docSieved, relTypeMapping));
		
		//Applying temporal classifiers...
		if (isClassifierSieve()) {
			
			// Init the classifier...
			EventDctTemporalClassifier edCls = new EventDctTemporalClassifier(taskName, "liblinear");
			EventTimexTemporalClassifier etCls = new EventTimexTemporalClassifier(taskName, "liblinear");
			EventEventTemporalClassifier eeCls = new EventEventTemporalClassifier(taskName, "liblinear");
			
			if (Arrays.asList(labels).contains("VAGUE")) {	//TimeBank-Dense --- Logistic Regression!
				edCls = new EventDctTemporalClassifier(taskName, "liblinear");
				etCls = new EventTimexTemporalClassifier(taskName, "logit");
				eeCls = new EventEventTemporalClassifier(taskName, "logit");
			} 
			
			//Init the feature vectors...	
			Map<String, String> ttlinks = null, etlinks = null;
			if (isTTFeature()) ttlinks = TimexTimexTemporalRule.getTimexTimexRuleRelation(doc);
			if (isETFeature()) {
				etlinks = docSieved.getTlinkTypes();
				if (Arrays.asList(labels).contains("VAGUE")) 	//TimeBank-Dense --- etlinks only from E-D rule labels 
					etlinks = EventDctTemporalRule.getEventDctRuleRelation(doc, true);
			}
			
			List<PairFeatureVector> edFv = EventDctTemporalClassifier.getEventDctTlinksPerFile(doc, edCls, 
					false, isGoldCandidate(), Arrays.asList(labels));
			List<PairFeatureVector> etFv = EventTimexTemporalClassifier.getEventTimexTlinksPerFile(doc, etCls, 
					false, isGoldCandidate(), Arrays.asList(labels), ttlinks);
			List<PairFeatureVector> eeFv = EventEventTemporalClassifier.getEventEventTlinksPerFile(doc, eeCls, 
					false, isGoldCandidate(), Arrays.asList(labels), etlinks);
			
			List<String> edClsLabels = edCls.predict(edFv, getEDModelPath(), labels);
			List<String> etClsLabels = etCls.predict(etFv, getETModelPath(), labels);
			List<String> eeClsLabels = eeCls.predict(eeFv, getEEModelPath(), labels);
			
			if (isRuleSieve()) {
				
				for (int i = 0; i < edFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < etFv.size(); i ++) {
					//Find label according to rules
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(etfv.getE1().getID() + "," + etfv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(etfv.getE2().getID() + "," + etfv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
					}							
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					//Find label according to rules
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					if (!docSieved.getTlinkTypes().containsKey(eefv.getE1().getID() + "," + eefv.getE2().getID())
							&& !docSieved.getTlinkTypes().containsKey(eefv.getE2().getID() + "," + eefv.getE1().getID())) {
						docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
					}				
				}
				
			} else {
				for (int i = 0; i < edFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(edFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), edClsLabels.get(i)));
				}
				for (int i = 0; i < etFv.size(); i ++) {
					EventTimexFeatureVector etfv = new EventTimexFeatureVector(etFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(etfv.getE1().getID(), etfv.getE2().getID(), etClsLabels.get(i)));
				}
				for (int i = 0; i < eeFv.size(); i ++) {
					EventEventFeatureVector eefv = new EventEventFeatureVector(eeFv.get(i));
					docSieved.getTlinks().add(new TemporalRelation(eefv.getE1().getID(), eefv.getE2().getID(), eeClsLabels.get(i)));
				}
			}
		}
		
		// Temporal links to string
		resultList.add(relationToString(doc, docSieved, relTypeMapping));
		
		return resultList;
	}
	
	public TLINK relationToString(Doc gold, Doc system, Map<String, String> relTypeMapping) {
		TLINK links = new TLINK();
		Set<String> extracted = new HashSet<String>();
		
		for (TemporalRelation tlink : system.getTlinks()) {
			
			Entity e1 = system.getEntities().get(tlink.getSourceID());
			Entity e2 = system.getEntities().get(tlink.getTargetID());
			if (e1 != null && e2 != null) {
				PairFeatureVector fv = new PairFeatureVector(system, e1, e2, tlink.getRelType(), null, null);
				
				for (String key : relTypeMapping.keySet()) {
					fv.setLabel(fv.getLabel().replace(key, relTypeMapping.get(key)));
				}
				
				String label = "NONE";
				if (gold.getTlinkTypes().containsKey(e1.getID() + "," + e2.getID())) {
					label = gold.getTlinkTypes().get(e1.getID() + "," + e2.getID());
				} 
				
				if (!extracted.contains(e1.getID()+","+e2.getID())
//						&& !label.equals("NONE")
						) {
					if (fv.getPairType().equals(PairType.timex_timex)) {
						links.getTT().add(system.getFilename()
								+ "\t" + e1.getID()
								+ "\t" + e2.getID()
								+ "\t" + label
								+ "\t" + fv.getLabel());
						extracted.add(e1.getID()+","+e2.getID());
						extracted.add(e2.getID()+","+e1.getID());
						
					} else if (fv.getPairType().equals(PairType.event_timex)) {
						EventTimexFeatureVector etfv = new EventTimexFeatureVector(fv);
						if (((Timex) etfv.getE2()).isDct()) {
							links.getED().add(system.getFilename()
									+ "\t" + e1.getID()
									+ "\t" + e2.getID()
									+ "\t" + label
									+ "\t" + fv.getLabel());
						} else {
							links.getET().add(system.getFilename()
									+ "\t" + e1.getID()
									+ "\t" + e2.getID()
									+ "\t" + label
									+ "\t" + fv.getLabel());
						}
						extracted.add(e1.getID()+","+e2.getID());
						extracted.add(e2.getID()+","+e1.getID());
						
					} else if (fv.getPairType().equals(PairType.event_event)) {
						links.getEE().add(system.getFilename()
								+ "\t" + e1.getID()
								+ "\t" + e2.getID()
								+ "\t" + label
								+ "\t" + fv.getLabel());
						extracted.add(e1.getID()+","+e2.getID());
						extracted.add(e2.getID()+","+e1.getID());
						
					}
				}
			}
		}
		
		return links;
	}
	
	public static String getRelTypeTimeBankDense(String type) {
		switch(type) {
			case "s": return "SIMULTANEOUS";
			case "b": return "BEFORE";
			case "a": return "AFTER";
			case "i": return "INCLUDES";
			case "ii": return "IS_INCLUDED";
			default: return "VAGUE";
		}
	}
	
//	public static Map<String, Map<String, String>> getTimeBankDenseTlinks(String tlinkPath, boolean columnFormat) throws Exception {
//		Map<String, Map<String, String>> tlinkPerFile = new HashMap<String, Map<String, String>>();
//		
//		BufferedReader br = new BufferedReader(new FileReader(new File(tlinkPath)));
//		String line;
//		String filename, e1, e2, tlink;
//	    while ((line = br.readLine()) != null) {
//	    	String[] cols = line.split("\t");
//	    	filename = cols[0] + ".tml";
//	    	if (columnFormat) filename = cols[0] + ".col";
//	    	e1 = cols[1]; e2 = cols[2];
//	    	if (e1.startsWith("t")) e1 = e1.replace("t", "tmx");
//	    	if (e2.startsWith("t")) e2 = e2.replace("t", "tmx");
//	    	tlink = getRelTypeTimeBankDense(cols[3]);
//	    	
//	    	if (!tlinkPerFile.containsKey(filename)) {
//	    		tlinkPerFile.put(filename, new HashMap<String, String>());
//	    	}
//    		tlinkPerFile.get(filename).put(e1+","+e2, tlink);
//	    }
//	    br.close();
//		
//		return tlinkPerFile;
//	}
	
	public static Map<String, Map<String, String>> getLinksFromFile(String tlinkPath) throws Exception {
		Map<String, Map<String, String>> tlinkPerFile = new HashMap<String, Map<String, String>>();
		
		BufferedReader br = new BufferedReader(new FileReader(new File(tlinkPath)));
		String line;
		String filename, e1, e2, tlink;
	    while ((line = br.readLine()) != null) {
	    	String[] cols = line.split("\t");
	    	filename = cols[0];
	    	e1 = cols[1]; e2 = cols[2];
	    	tlink = cols[3];
	    	
	    	if (!tlinkPerFile.containsKey(filename)) {
	    		tlinkPerFile.put(filename, new HashMap<String, String>());
	    	}
    		tlinkPerFile.get(filename).put(e1+","+e2, tlink);
	    }
	    br.close();
		
		return tlinkPerFile;
	}

	public Temporal(String[] relTypes, String edModelPath, String etModelPath, String eeModelPath) {
		
		this.setRelTypes(relTypes);
		
		ensureDirectory(new File(edModelPath).getParentFile());
		ensureDirectory(new File(etModelPath).getParentFile());
		ensureDirectory(new File(eeModelPath).getParentFile());
		
		this.setEDModelPath(edModelPath);
		this.setETModelPath(etModelPath);
		this.setEEModelPath(eeModelPath);
		
		this.setRuleSieve(true);
		this.setClassifierSieve(true);
		this.setReasoner(true);
		
		this.setTTFeature(true);
		this.setETFeature(false);	
		
		this.setGoldCandidate(true);
	}
	
	public Temporal(boolean goldCandidate, String[] relTypes, 
			String edModelPath, String etModelPath, String eeModelPath) {
		
		this(relTypes, edModelPath, etModelPath, eeModelPath);
		this.setGoldCandidate(goldCandidate);
	}
	
	public Temporal(boolean goldCandidate, String[] relTypes, 
			String edModelPath, String etModelPath, String eeModelPath,
			boolean ruleSieve, boolean classifierSieve, boolean reasoner) {
		
		this(goldCandidate, relTypes, edModelPath, etModelPath, eeModelPath);
		this.setRuleSieve(ruleSieve);
		this.setClassifierSieve(classifierSieve);
		this.setReasoner(reasoner);
	}
	
	public Temporal(boolean goldCandidate, String[] relTypes, 
			String edModelPath, String etModelPath, String eeModelPath,
			boolean ruleSieve, boolean classifierSieve, boolean reasoner,
			boolean ttFeature, boolean etFeature) {
		
		this(goldCandidate, relTypes, edModelPath, etModelPath, eeModelPath,
				ruleSieve, classifierSieve, reasoner);
		this.setTTFeature(ttFeature);
		this.setETFeature(etFeature);
	}
	
	public boolean isGoldCandidate() {
		return goldCandidate;
	}

	public void setGoldCandidate(boolean goldCandidate) {
		this.goldCandidate = goldCandidate;
	}

	public boolean isRuleSieve() {
		return ruleSieve;
	}

	public void setRuleSieve(boolean ruleSieve) {
		this.ruleSieve = ruleSieve;
	}

	public boolean isClassifierSieve() {
		return classifierSieve;
	}

	public void setClassifierSieve(boolean classifierSieve) {
		this.classifierSieve = classifierSieve;
	}

	public boolean isReasoner() {
		return reasoner;
	}

	public void setReasoner(boolean reasoner) {
		this.reasoner = reasoner;
	}
	
	public boolean isTTFeature() {
		return ttFeature;
	}

	public void setTTFeature(boolean ttFeature) {
		this.ttFeature = ttFeature;
	}
	
	public boolean isETFeature() {
		return etFeature;
	}

	public void setETFeature(boolean etFeature) {
		this.etFeature = etFeature;
	}

	public String getEDModelPath() {
		return edModelPath;
	}

	public void setEDModelPath(String edModelPath) {
		this.edModelPath = edModelPath;
	}

	public String getETModelPath() {
		return etModelPath;
	}

	public void setETModelPath(String etModelPath) {
		this.etModelPath = etModelPath;
	}

	public String getEEModelPath() {
		return eeModelPath;
	}

	public void setEEModelPath(String eeModelPath) {
		this.eeModelPath = eeModelPath;
	}

	public String[] getRelTypes() {
		return relTypes;
	}

	public void setRelTypes(String[] relTypes) {
		this.relTypes = relTypes;
	}
	
	public static void ensureDirectory(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
}
