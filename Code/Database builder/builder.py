key = "TTJITK9L"

def get_words(coords):
	import urllib.request, json
	base = "https://api.what3words.com/v2/reverse?coords={}%2C{}&key={}"

	a = json.loads(urllib.request.urlopen(base.format(coords[0], coords[1], key)).read())

	return a["words"]


def number_of_shelters(city):
	import json
	db = json.load(open("../../datasets/shelters.json", "r", encoding="utf-8"))

	city_data = []
	for obj in db["features"]:
		if obj["properties"]["kommune"].lower() == city.lower():
			city_data.append(obj)

	return len(city_data)


def map_all_shelters():
	import json
	
	db = json.load(open("../../datasets/shelters.json", "r", encoding="utf-8"))

	city_data = {}

	for obj in db["features"]:

		kommune = obj["properties"]["kommune"].lower()
		
		if kommune in city_data:
			city = city_data[kommune]
			city["antall"] += 1
		else:
			city = {}
			city["antall"] = 1
			city["geo"] = obj["geometry"]["coordinates"]

		city_data[kommune] = city

	open("../../datasets/custom/tilflukt.json", "w", encoding="utf-8").write(json.dumps(city_data, sort_keys=True, indent=4))
	return True

def radioactivity():
	from xml.dom import minidom
	import json

	xmldoc = minidom.parse("../../datasets/radnett.xml")
	itemlist = xmldoc.getElementsByTagName('station')

	radios = {}
	for item in itemlist:
		city = item.attributes["name"].value
		coords = [item.attributes["lat"].value, item.attributes["lon"].value]
		measurement = item.getElementsByTagName("value")
		radios[city] = {}

		radios[city]["geo"] = {"lat": coords[0], "lng": coords[1]}
		times = []
		for m in measurement:
			time = m.attributes["starttime"].value
			m = str(m.childNodes)
			m = m.replace("[<DOM Text node \"'", "").replace("'\">]", "")
			t = {}
			t[time] = m
			times.append(t)
		radios[city]["timestamp"] = times

	open("../../datasets/custom/rad.json", "w", encoding="utf-8").write(json.dumps(radios, sort_keys=True, indent=4))
	return True


if __name__ == "__main__":
	a = radioactivity()
	print(a)