import requests, json

# Importing ElementTree which I use to create Elements
from xml.etree import ElementTree
# Importing minidom which I use to write a readable version of the XML
from xml.dom import minidom


client_id = "1a92af37-bf52-4b96-baae-1a25ba197caf"
client_secret = "0d28ef08-3918-4dff-b721-13afa8a23a98"

def get_lightning(start, end, lat, lng):
	import re
	reg = r"\d{4}-\d\d-\d\d"

	root = ElementTree.Element("root")

	r = requests.get(f"https://frost.met.no/lightning/v0.ualf?referencetime={start}%2F{end}&geometry=nearest(POINT({lng}%20{lat}))", auth=(client_id, client_secret))
	r = r.text.split("\n")
	print(r)
	res = []
	for line in r:
		if len(line) == 0:
			continue
		line = line.split()[1:5] + line.split()[8:10]

		obj = {}

		lightning = ElementTree.Element("lightning")

		attrs = ["year", "month", "day", "hour", "lat", "lng"]
		print(len(attrs), len(line))
		for i in range(0, 5):
			lightning.set(attrs[i], line[i])	
			root.append(lightning)	

		#obj["year"], obj["month"], obj["day"], obj["hour"], obj["lat"], obj["lng"] = line
		res.append(obj)

	return root

def get_sources():
	ans = {}

	rad = json.loads(open("../../datasets/custom/rad.json", "r", encoding="utf-8").read())


	for city in rad:
		lng = rad[city]["geo"]["lng"]
		lat = rad[city]["geo"]["lat"]
		
		r = requests.get(f"https://frost.met.no/sources/v0.jsonld?geometry=nearest(POINT({lng}%20{lat}))&nearestmaxcount=1", auth=(client_id, client_secret))
		try:
			data = json.loads(r.text)["data"]
		except:
			print("failed", city)
		ids = []
		for element in data:
			ids.append(element["id"])
		ans[city] = ids

	return ans


def get_weather(id, city, start, end):
	r = requests.get(f"https://frost.met.no/observations/v0.jsonld?sources={id}&referencetime={start}%2F{end}&elements=air_temperature%2C%20sum(precipitation_amount P1D)", auth=(client_id, client_secret))
	r = json.loads(r.text)

	root = ElementTree.Element("weather")
	root.set("station", id)
	try:
		data = r["data"]
	except:
		print(f"failed {id}\n", r)
		return None

	temps = []
	prec = []

	for day in data:
		date_stamp, time_stamp = day["referenceTime"].split("T")
		
		if root.find(f"date[@date-stamp='{date_stamp}']"):
			date = root.find(f"date[@date-stamp='{date_stamp}']")
		else:
			date = ElementTree.Element("date")
			date.set("date-stamp", date_stamp)

		time = ElementTree.Element("time")
		time.set("time-stamp", time_stamp)

		for point in day["observations"]:
			if point["elementId"] == "air_temperature":
				temp = ElementTree.Element("air_temperature")
				temp.set("unit",str(point["unit"]))
				temp.text = str(point["value"])
				time.append(temp)
			else:
				prec = ElementTree.Element("precipitation_amount")
				prec.set("unit", str(point["unit"]))
				prec.text = str(point["value"])
				time.append(prec)
		date.append(time)
		if not root.find(f"date[@date-stamp='{date_stamp}']"):
			root.append(date)

	return root

#print(get_lightning("2019-01-01", "2019-05-07"))
#open("sources.json", "w", encoding="utf-8").write(json.dumps(get_sources(), indent=4))

def get_weathers(start, end):
	rad = json.loads(open("sources.json", "r", encoding="utf-8").read())

	weather = ElementTree.Element("weather")

	for city in rad:
		station = rad[city][0]
		print(city)
		city = get_weather(station, city, start, end)
		if city is None:
			continue
		weather.append(city)

	weather_string = ElementTree.tostring(weather, "utf-8", method="xml")
	weather = minidom.parseString(weather_string).toprettyxml()

	open("../../datasets/custom/dataset.xml", "w", encoding="utf-8").write(weather)


def get_shelters(city):
	shelters = json.loads(open("../../datasets/custom/tilflukt.json", "r", encoding="utf-8").read())
	root = ElementTree.Element("shelter")
	try:
		lat, lng = shelters[city.lower()]["geo"]
		root.set("longitude", str(lat))
		root.set("latitude", str(lng))
		root.text = str(shelters[city.lower()]["antall"])
		return root
	except:
		print("shelters", city.lower())
		return None

def save_xml(name, tree):
	xml_string = ElementTree.tostring(tree, "utf-8", method="xml")
	tree = minidom.parseString(xml_string).toprettyxml()

	prefix = prefix_rdf()

	open(f"../../datasets/custom/{name}", "w", encoding="utf-8").write(tree)

def prefix_rdf():
	rdf = ElementTree.Element("rdf:RDF")

	prefixes = {
		"event": "http://purl.org/NET/c4dm/event.owl#",
        "dbr": "http://dbpedia.org/resource/",
        "dbo": "http://dbpedia.org/ontology/",
        "time": "http://www.w3.org/2006/time#",
        "timeline": "http://purl.org/NET/c4dm/timeline.owl#",
        "foaf": "http://xmlns.com/foaf/0.1/",
        "geo": "http://www.georss.org/georss/"
	}

	for prefix, URI in prefixes.items():
		rdf.set(prefix, URI)

	owl = ElementTree.Element("owl:Ontology")
	owl.set("rdf:about", "http://mvrkws.com/ontology")

	rdf.append(owl)

	streng = ElementTree.tostring(rdf, encoding="utf-8").decode()

	return streng

def domains_rdf():

	return None

def build_xml(start, end):
	sources = json.loads(open("sources.json", "r", encoding="utf-8").read())
	print(sources)

	root = ElementTree.Element("root")

	for city in sources:
		print(city)
		city_element = ElementTree.Element("city")
		city_element.set("name", city)
		shelters = get_shelters(city)
		if shelters is not None:
			city_element.append(shelters)

		weather = get_weather(sources[city][0], city, start, end)
		if weather is not None:
			city_element.append(weather)

		radiation = get_radiation(city)
		city_element.append(radiation)

		root.append(city_element)

	save_xml("dataset_datetime.xml", root)

def get_radiation(city):
	rad = json.loads(open("../../datasets/custom/rad.json", "r", encoding="utf-8").read())

	root = ElementTree.Element("radiation")

	city = rad[city]["timestamp"]

	for measurement in city:
		date_stamp, time_stamp = list(measurement.keys())[0].split("T")

		if root.find(f"date[@date-stamp='{date_stamp}']"):
			date = root.find(f"date[@date-stamp='{date_stamp}']")
		else:
			date = ElementTree.Element("date")
			date.set("date-stamp", date_stamp)
		
		time = ElementTree.Element("time")
		time.set("time-stamp", time_stamp)
		
		ms = ElementTree.Element("measurement")
		ms.set("type", "radiation")
		ms.text = list(measurement.values())[0]

		time.append(ms)

		date.append(time)
		
		if not root.find(f"date[@date-stamp='{date_stamp}']"):
			root.append(date)

	return root

start = "2019-05-07"
end = "2019-05-08"
build_xml("2019-05-07", "2019-05-08")
#get_weathers("2019-01-01", "2019-05-01")