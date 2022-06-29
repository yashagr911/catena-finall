# CATENA

Detecting events and their relationships has become a crucial part of natural language processing. It has endless applications in the information research field while still being a highly challenging task. An event can be defined as something that happened. It can have many questions associated with it, for instance, when, where, how, by whom, etc. The idea of event detection is to identify the events and their relationships in unstructured data. The relationships between two events can be further classified into temporal and causal. The events are temporal when they are time-related. For instance, “We went to the school today”, “he slept at 9 pm tonight”. Here, “9 pm tonight” is included in “today” and hence going to school and sleeping are temporally related. Whereas, when one relation is caused by the other, the type of the relationship is called a causal relationship, for example, “His brakes failed and he had an accident”. Here, failure of the brakes caused the accident hence the events are causally related. 

Several techniques have been proposed so far for the detection of relationships that are either exclusively based on machine learning, linguistic markers, or rule-based. In this project, we have studied a sieve-based model CATENA [2], which deals with both machine learning and a rule-based sieve. CATENA has a multiclass architecture for the extraction and classification of both temporal and causal events.

CATENA takes any annotated document with DCT, events, and timexs as an input and outputs the same document with temporal and causal links between the events in CATENA. DCT stands for Document Creation Time for the respective document, an event can be interpreted as any occurrence such as “eating”, “establishing”, etc and timexs represent any temporal information, for example, “2AM”, “friday”, “2022”,etc. The model outputs the same document with temporal and causal links between each event for CATENA. 

This Project is the replication of https://github.com/paramitamirza/CATENA. The goal of this project was to understand the working of the model in depth and fine tune it.

## Steps

1. Clone the repository.
2. place the wnjpn.db file:
   wnjpn.db can be found in the following link:
   https://drive.google.com/file/d/1H7BOxzXGV0Y9TRilac6pxWWr6F8lkKEW/view?usp=sharing
   Instrucion: Download the above file. Place it in the directory: CATENA-finall/resourse/wnjpn.db
3. run the project on intellij. 
4. ```
5.Choose your parameters.
 -i,--input <arg>        Input TimeML file/directory path
 -f,--col                (optional) Input files are in column format (.col)
 -tl,--tlinks <arg>      (optional) Input file containing list of gold temporal links
 -cl,--clinks <arg>      (optional) Input file containing list of gold causal links
 -gl,--gold              (optional) Gold candidate pairs to be classified are given
 -y,--clinktype          (optional) Output the type of CLINK (ENABLE, PREVENT, etc.) from the rule-based sieve
        
 -x,--textpro <arg>      TextPro directory path
 -l,--matelemma <arg>    Mate tools' lemmatizer model path   
 -g,--matetagger <arg>   Mate tools' PoS tagger model path
 -p,--mateparser <arg>   Mate tools' parser model path      
 
 -t,--ettemporal <arg>   CATENA model path for E-T temporal classifier    
 -d,--edtemporal <arg>   CATENA model path for E-D temporal classifier                       
 -e,--eetemporal <arg>   CATENA model path for E-E temporal classifier
 -c,--eecausal <arg>     CATENA model path for E-E causal classifier
 
 -b,--train              (optional) Train the models
 -m,--tempcorpus <arg>   (optional) Directory path (containing .tml or .col files) for training temporal classifiers
 -u,--causcorpus <arg>   (optional) Directory path (containing .tml or .col files) for training causal classifier     
  
  For example:
  java -Xmx2G -jar ./target/CATENA-1.0.2.jar -i ./data/example_COL/ --col --tlinks ./data/TempEval3.TLINK.txt --clinks ./data/Causal-TimeBank.CLINK.txt -l ./models/CoNLL2009-ST-English-ALL.anna-3.3.lemmatizer.model -g ./models/CoNLL2009-ST-English-ALL.anna-3.3.postagger.model -p ./models/CoNLL2009-ST-English-ALL.anna-3.3.parser.model -x ./tools/TextPro2.0/ -d ./models/catena-event-dct.model -t ./models/catena-event-timex.model -e ./models/catena-event-event.model -c ./models/catena-causal-event-event.model -b -m ./data/Catena-train_COL/ -u ./data/Causal-TimeBank_COL/```

5. This will take the input from the folder CATENA-finall/data/Example-col/ as the test data and will produce the output(with relations) on the console.
6. To test accuracy, run Catena-finall\src\catena\embedding\experiments\EvaluateTimeBankDenseCrossVal.java
7. Voila, we have a working model.
