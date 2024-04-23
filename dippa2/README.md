# Project ToDo:s

- få ML model och runna korrekt --- DONE ---
- ta ML model output i react native och sätt in i kalender. 
	- skapa en kalender med månadsformat som det är, men dagsformat är en "graf" som i FirstBeat appen.
		- en datapunkt är 2 sec, var datapunkten motsvarar en färg, finns lika många färger som labels.
- data sparas som "timestamp" : label, t.ex. 1713896010736:2, 1713896011736:4, 1713896012736:2 (också möjlighet spara det enligt isoformat, om det är bättre)
	- sparas i appdata på lokala telefon
	- blir läst in i ActivitiesScreen
- fixa lite finare UI, bara med prompts till Cursor, kan googla/youtube "Top UI:s React Native"
- Göra en undersökning med mamma&pappa och resten äkta/fejk
- skriva arbetet, sikta på 60 sidor?

add ons:
- få "scan sensors", "connect to sensors" funktionalitet till React Native istället för natively. 
	- ingen live data behövs
	- sensorerna som connectats till sparas i en lsita som visas "Connected sensors"