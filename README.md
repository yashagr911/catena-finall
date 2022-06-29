# CATENA

Detecting events and their relationships has become a crucial part of natural language processing. It has endless applications in the information research field while still being a highly challenging task. An event can be defined as something that happened. It can have many questions associated with it, for instance, when, where, how, by whom, etc. The idea of event detection is to identify the events and their relationships in unstructured data. The relationships between two events can be further classified into temporal and causal. The events are temporal when they are time-related. For instance, “We went to the school today”, “he slept at 9 pm tonight”. Here, “9 pm tonight” is included in “today” and hence going to school and sleeping are temporally related. Whereas, when one relation is caused by the other, the type of the relationship is called a causal relationship, for example, “His brakes failed and he had an accident”. Here, failure of the brakes caused the accident hence the events are causally related. 

Several techniques have been proposed so far for the detection of relationships that are either exclusively based on machine learning, linguistic markers, or rule-based. In this project, we have studied a sieve-based model CATENA [2], which deals with both machine learning and a rule-based sieve. CATENA has a multiclass architecture for the extraction and classification of both temporal and causal events.

CATENA takes any annotated document with DCT, events, and timexs as an input and outputs the same document with temporal and causal links between the events in CATENA. DCT stands for Document Creation Time for the respective document, an event can be interpreted as any occurrence such as “eating”, “establishing”, etc and timexs represent any temporal information, for example, “2AM”, “friday”, “2022”,etc. The model outputs the same document with temporal and causal links between each event for CATENA. 

wnjpn.db can be found in the following link:
https://drive.google.com/file/d/1H7BOxzXGV0Y9TRilac6pxWWr6F8lkKEW/view?usp=sharing
Instrucion: Download the above file. Place it in the directory: CATENA-finall/resourse/wnjpn.db
