package catena.evaluator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import catena.parser.entities.Doc;
import catena.parser.entities.Entity;
import catena.parser.entities.Timex;

public class PairEvaluator {

	public static double Mypr = 0,Myre = 0,Myf1 = 0;
	
	private List<String> pairs;
	
	public PairEvaluator(List<String> pairs) {
		setPairs(pairs);
	}
	
	public double accuracy() {
		int eeCorrect = 0;
		int eeInstance = 0;
		for (String s : pairs) { //e1	e2	label	predicted
			if (!s.isEmpty()) {
				String[] cols = s.split("\t");
				if (cols[2].equals(cols[3])) eeCorrect += 1;
				eeInstance += 1;
			}
		}
		return (double)eeCorrect/(double)eeInstance;
	}
	
	public int numCorrect() {
		int eeCorrect = 0;
		for (String s : pairs) { //e1	e2	label	predicted
			if (!s.isEmpty()) {
				String[] cols = s.split("\t");
				if (cols[2].equals(cols[3])) eeCorrect += 1;
			}
		}
		return eeCorrect;
	}
	
	public int numInstance() {
		int eeInstance = 0;
		for (String s : pairs) { //e1	e2	label	predicted
			if (!s.isEmpty()) {
				eeInstance += 1;
			}
		}
		return eeInstance;
	}
	
	public void evaluatePerLabel() {
		int[] tp = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		int[] fp = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		int[] fn = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		int[] total = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		List<String> labelList = Arrays.asList(label);
		
		int idxLabel, idxPred;
		for (String s : pairs) { //e1	e2	label(str)	predicted(str)
			if (!s.trim().isEmpty()) {
				String[] cols = s.split("\t");
				idxLabel = labelList.indexOf(cols[2]);
				if (!cols[2].equals("NONE")) total[idxLabel] ++;
				if (cols[3].equals("NONE")) {
					fn[idxLabel] ++;
				} else {
					idxPred = labelList.indexOf(cols[3]);
					if (cols[2].equals("NONE")) fp[idxPred] ++;
					else {
						if (idxPred == idxLabel) tp[idxPred] ++;
						else fp[idxPred] ++;
					}
				}
			}
		}
		printEvaluation(label, tp, fp, total);
//		printEvaluation(label, tp, fp, fn, total);
	}
	
	public void evaluatePerLabelIdx() {
		int[] tp = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		int[] fp = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		int[] total = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		String[] label = {"BEFORE", "AFTER", "IBEFORE", "IAFTER", "IDENTITY", "SIMULTANEOUS", 
				"INCLUDES", "IS_INCLUDED", "DURING", "DURING_INV", "BEGINS", "BEGUN_BY", "ENDS", "ENDED_BY"};
		
		int idxLabel, idxPred;
		for (String s : pairs) { //label(idx)	predicted(idx)
			if (!s.isEmpty()) {
				String[] cols = s.split("\t");
				idxLabel = Integer.parseInt(cols[0])-1;
				idxPred = Integer.parseInt(cols[1])-1;
				total[idxLabel] ++;
				if (idxPred == idxLabel) tp[idxPred] ++;
				else fp[idxPred] ++;
			}
		}
		printEvaluation(label, tp, fp, total);
	}
	
	public void evaluatePerLabelIdx(String[] label) {
		int[] tp = new int[label.length]; Arrays.fill(tp, 0);
		int[] fp = new int[label.length]; Arrays.fill(fp, 0);
		int[] total = new int[label.length]; Arrays.fill(total, 0);
		
		int idxLabel, idxPred;
		for (String s : pairs) { //label(idx)	predicted(idx)
			if (!s.isEmpty()) {
				String[] cols = s.split("\t");
				idxLabel = Integer.parseInt(cols[0])-1;
				idxPred = Integer.parseInt(cols[1])-1;
				total[idxLabel] ++;
				if (idxPred == idxLabel) tp[idxPred] ++;
				else fp[idxPred] ++;
			}
		}
		printEvaluation(label, tp, fp, total);
	}
	
	public void evaluatePerLabel(String[] label) {
		int[] tp = new int[label.length]; Arrays.fill(tp, 0);
		int[] fp = new int[label.length]; Arrays.fill(fp, 0);
		int[] fn = new int[label.length]; Arrays.fill(fp, 0);
		int[] total = new int[label.length]; Arrays.fill(total, 0);
		
		List<String> labelList = Arrays.asList(label);
		
		int idxLabel, idxPred;
		for (String s : pairs) { //filename		e1	e2	label(str)	predicted(str)
			if (!s.trim().isEmpty()) {
				String[] cols = s.split("\t");
				idxLabel = labelList.indexOf(cols[3]);
				if (!cols[3].equals("NONE")) total[idxLabel] ++;
				if (cols[4].equals("NONE")) {
					fn[idxLabel] ++;
				} else {
					idxPred = labelList.indexOf(cols[4]);
					if (cols[3].equals("NONE")) fp[idxPred] ++;
					else {
						if (idxPred == idxLabel) tp[idxPred] ++;
						else fp[idxPred] ++;
					}
				}
			}
		}
		printEvaluation(label, tp, fp, total);
//		printEvaluation(label, tp, fp, fn, total);
	}
	
	public void evaluateCausalPerLabelIdx(String[] label) {
		int[] tp = new int[label.length]; Arrays.fill(tp, 0);
		int[] fp = new int[label.length]; Arrays.fill(fp, 0);
		int[] total = new int[label.length]; Arrays.fill(total, 0);
		int sumtp = 0;
		int sumfp = 0;
		int sumtn = 0;
		int sumfn = 0;
		
		int idxLabel, idxPred;
		for (String s : pairs) { //label(idx)	predicted(idx)
			if (!s.isEmpty()) {
				String[] cols = s.split("\t");
				idxLabel = Integer.parseInt(cols[0])-1;
				idxPred = Integer.parseInt(cols[1])-1;
				total[idxLabel] ++;
				if (!label[idxPred].equals("NONE")) { 
					if (idxPred == idxLabel) {
						tp[idxPred] ++;
						sumtp ++;
					} else {
						fp[idxPred] ++;
						sumfp ++;
					}
				}
				else {
					if (!label[idxLabel].equals("NONE")) sumfn ++;
					else {
						if (idxPred == idxLabel) {
							tp[idxPred] ++;
							sumtn ++;
						}
					}
				}
			}
		}
		
		System.err.println(label[0] + "\t" +
				(total[0]+total[1]) + "\t" +
				(tp[0]+tp[1]) + "\t" +
				(fp[0]+fp[1]));
		
//		printEvaluation(label, tp, fp, total);
//		double precision = sumtp/(double)(sumtp+sumfp);
//		double recall = sumtp/(double)(sumtp+sumfn);
//		double f1 = (2*precision*recall)/(precision+recall);
//		double tpr = recall;
//		double fpr = sumfp/(double)(sumfp+sumtn);
//		//System.out.println("P R F1 TPR FPR")
//		System.err.println(precision +
//				" " + recall +
//				" " + f1 +
//				" " + tpr + 
//				" " + fpr);
	}
	
	public void printEvaluation(String[] label,
			int[] tp, int[] fp, int[] total) {
		System.out.println("here");
		double[] precision = new double[label.length]; Arrays.fill(precision, 0);
		double[] recall = new double[label.length]; Arrays.fill(recall, 0);
		double[] f1 = new double[label.length]; Arrays.fill(f1, 0);
		double totalf1 = 0;
		double totalp = 0;
		double totalr = 0;
		int totaltp = 0;
		int totaltotal = 0;
		double totalwp = 0;
		double totalwr = 0;
		double totalwf1 = 0;

		for (int i=0; i<label.length; i++) {
			if ((tp[i]+fp[i]) > 0) {
				precision[i] = tp[i]/(double)(tp[i]+fp[i]);
			} else {
				precision[i] = 0;
			}
			if (total[i] > 0) {
				recall[i] = tp[i]/(double)total[i];
			} else {
				recall[i] = 0;
			}
			if ((precision[i]+recall[i]) > 0) {
				f1[i] = (2*precision[i]*recall[i])/(double)(precision[i]+recall[i]);
			} else {
				f1[i] = 0;
			}
			totalp += precision[i];
			totalr += recall[i];
			totalf1 += f1[i];
			totaltp += tp[i];
			totaltotal += total[i];
			totalwp += precision[i]*total[i];
			totalwr += recall[i]*total[i];
			totalwf1 += f1[i]*total[i];
		}
		
		for (int i=0; i<label.length; i++) {
			System.out.println(label[i] + "\t" +
					total[i] + "\t" +
					tp[i] + "\t" +
					fp[i] + "\t" +
					precision[i] + "\t" +
					recall[i] + "\t" +
					f1[i]);
		}
		Mypr += totalp/label.length;
		Myre += totalr/label.length;
		Myf1 += totalf1/label.length;
//		System.out.println("Total Precision " + totalp/label.length +
//				" Total recall " + totalr/label.length +
//				" F1 " + totalf1/label.length);
		System.out.println("Avg " + totalp/label.length +
				" " + totalr/label.length +
				" " + totalf1/label.length);
		System.out.println("W-Avg " + totalwp/(double)totaltotal + 
				" " + totalwr/(double)totaltotal +
				" " + totalwf1/(double)totaltotal);
//		System.out.println("Total accuracy " + totaltp/(double)(totaltp+totalfp) + " (" + totaltp + "/" + totaltotal + ")");
		System.out.println("Total recall " + totaltp/(double)totaltotal + " (" + totaltp + "/" + totaltotal + ")");
	}
	
	private void printEvaluation(String[] label,
			int[] tp, int[] fp, int[] fn, int[] total) {
		double[] precision = new double[label.length]; Arrays.fill(precision, 0);
		double[] recall = new double[label.length]; Arrays.fill(recall, 0);
		double[] f1 = new double[label.length]; Arrays.fill(f1, 0);
		double totalf1 = 0;
		double totalp = 0;
		double totalr = 0;
		int totaltp = 0;
		int totaltotal = 0;
		double totalwp = 0;
		double totalwr = 0;
		double totalwf1 = 0;
		
		for (int i=0; i<label.length; i++) {
			if ((tp[i]+fp[i]) > 0) {
				precision[i] = tp[i]/(double)(tp[i]+fp[i]);
			} else {
				precision[i] = 0;
			}
			if ((tp[i]+fn[i]) > 0) {
				recall[i] = tp[i]/(double)(tp[i]+fn[i]);
			} else {
				recall[i] = 0;
			}
			if ((precision[i]+recall[i]) > 0) {
				f1[i] = (2*precision[i]*recall[i])/(double)(precision[i]+recall[i]);
			} else {
				f1[i] = 0;
			}
			totalp += precision[i];
			totalr += recall[i];
			totalf1 += f1[i];
			totaltp += tp[i];
			totaltotal += (tp[i]+fn[i]);
			totalwp += precision[i]*(tp[i]+fn[i]);
			totalwr += recall[i]*(tp[i]+fn[i]);
			totalwf1 += f1[i]*(tp[i]+fn[i]);
		}
		
		for (int i=0; i<label.length; i++) {
			System.out.println(label[i] + "\t" +
					total[i] + "\t" +
					tp[i] + "\t" +
					fp[i] + "\t" +
					precision[i] + "\t" +
					recall[i] + "\t" +
					f1[i]);
		}
		System.out.println("Total Precision " + totalp/label.length +
				"Total Recall " + totalr/label.length +
				"F1 " + totalf1/label.length);
		System.out.println("Average " + totalp/label.length + 
				" " + totalr/label.length +
				" " + totalf1/label.length);
		System.out.println("Weighted Average " + totalwp/(double)totaltotal + 
				" " + totalwr/(double)totaltotal +
				" " + totalwf1/(double)totaltotal);
		System.out.println("Accuracy " + totaltp/(double)totaltotal + " (" + totaltp + "/" + totaltotal + ")");
	}
	
	public void printIncorrectAndSentence(Doc docCol) throws IOException {
		String label, pred, e1Str, e2Str, sentStr;
		Entity e1, e2;
		for (String s : pairs) { //e1	e2	label(str)	predicted(str)
			if (!s.isEmpty()) {
				String[] cols = s.split("\t");
				label = cols[2];
				pred = cols[3];
				e1 = docCol.getEntities().get(cols[0]);
				e2 = docCol.getEntities().get(cols[1]);
				
				sentStr = "";
				if (e1 instanceof Timex) {
					e1Str = ((Timex)e1).getValue();
					if (((Timex) e1).isDct()) {
						sentStr += "DCT";
					} else {
						sentStr += docCol.getSentences().get(e1.getSentID()).toString(docCol);
					}
				}
				else {
					e1Str = e1.toString(docCol);
					sentStr += docCol.getSentences().get(e1.getSentID()).toString(docCol);
				}
				if (e2 instanceof Timex) {
					e2Str = ((Timex)e2).getValue(); //+ "-" + ((Timex)e2).getType() + "-" + ((Timex)e2).isDct();
					if (((Timex) e2).isDct()) {
						sentStr += "DCT";
					} else {
						if ((e1 instanceof Timex && ((Timex) e1).isDct())
								|| !e1.getSentID().equals(e2.getSentID()))
							sentStr += " || " + docCol.getSentences().get(e2.getSentID()).toString(docCol);
					}
				}
				else {
					e2Str = e2.toString(docCol);
					if (!e1.getSentID().equals(e2.getSentID()))
						sentStr += " || " + docCol.getSentences().get(e2.getSentID()).toString(docCol);
				}
				
				if (!label.equals(pred)) {					
					System.err.println(e1Str + " | " + e2Str + " | " + sentStr + " | " + label + " | " + pred);
				} else {					
					System.out.println(e1Str + " | " + e2Str + " | " + sentStr + " | " + label + " | " + pred);
				}
			}
		}
	}

	public List<String> getPairs() {
		return pairs;
	}

	public void setPairs(List<String> pairs) {
		this.pairs = pairs;
	}

}
