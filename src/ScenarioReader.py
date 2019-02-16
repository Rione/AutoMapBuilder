import os
import xml.etree.ElementTree as ET


class ScenarioReader:
    def __init__(self, map_name: str):
        self.map_name = map_name

    def scenario_reader(self):
        tree = ET.ElementTree(file=os.getcwd().replace('/src', '') + '/map/' + self.map_name + '/map/scenario.xml')
        root = tree.getroot()

        for element in root:
            print(element)


reader = ScenarioReader('sakae')
reader.scenario_reader()
