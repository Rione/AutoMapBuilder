import os
import xml.etree.ElementTree as ET


class ScenarioReader:
    def __init__(self, map_name: str):
        self.map_name = map_name

    def scenario_reader(self):
        tree = ET.ElementTree(file=os.getcwd().replace('/src', '') + '/map/' + self.map_name + '/map/scenario.xml')
        root = tree.getroot()

        # シナリオを追加
        result = []
        for element in root:
            name = str(element.tag).replace('{urn:roborescue:map:scenario}', '')
            location = int(dict(element.attrib).get('{urn:roborescue:map:scenario}location'))
            result.append([name, location])

        return result


reader = ScenarioReader('sakae')
print(reader.scenario_reader())
