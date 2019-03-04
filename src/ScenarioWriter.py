import random
import xml.dom.minidom
import numpy as np


class ScenarioWriter:
    def __init__(self, path: str, nodes: dict, edges: dict, buildings: dict, roads: dict):
        self.path = path
        self.nodes = nodes
        self.edges = edges
        self.buildings = buildings
        self.roads = roads

    def choice_buildings(self, min_number: int, max_number: int):
        number = random.randint(min_number, max_number)
        keys = list(self.buildings.keys())
        return np.random.choice(keys, number, replace=False)

    def choice_roads(self, min_number: int, max_number: int):
        number = random.randint(min_number, max_number)
        keys = list(self.roads.keys())
        return np.random.choice(keys, number, replace=False)

    def choice_agents(self, min_number: int, max_number: int):
        number = random.randint(min_number, max_number)
        keys = list(self.buildings.keys()) + list(self.roads.keys())
        return np.random.choice(keys, number)

    def write(self):
        doc = xml.dom.minidom.Document()

        root = doc.createElement('scenario:scenario')
        subnode_attr = doc.createAttribute('xmlns:scenario')
        subnode_attr.value = 'urn:roborescue:map:scenario'
        doc.appendChild(root)
        root.setAttributeNode(subnode_attr)

        # refuge書き出し
        for building_id in self.choice_buildings(1, 3):
            refuge = doc.createElement('scenario:refuge')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(building_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # firestation書き出し
        for building_id in self.choice_buildings(0, 6):
            refuge = doc.createElement('scenario:firestation')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(building_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # policeoffice書き出し
        for building_id in self.choice_buildings(0, 6):
            refuge = doc.createElement('scenario:policeoffice')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(building_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # ambulancecentre書き出し
        for building_id in self.choice_buildings(0, 6):
            refuge = doc.createElement('scenario:ambulancecentre')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(building_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # gasstation書き出し
        for building_id in self.choice_buildings(0, 3):
            refuge = doc.createElement('scenario:gasstation')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(building_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # hydrant書き出し
        for road_id in self.choice_roads(0, 3):
            refuge = doc.createElement('scenario:hydrant')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(road_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # fire書き出し
        for fire_id in self.choice_buildings(1, 5):
            refuge = doc.createElement('scenario:fire')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(fire_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # civilian書き出し
        for agent_id in self.choice_agents(1, 120):
            refuge = doc.createElement('scenario:civilian')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(agent_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # firebrigade書き出し
        for agent_id in self.choice_agents(1, 120):
            refuge = doc.createElement('scenario:firebrigade')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(agent_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # policeforce書き出し
        for agent_id in self.choice_agents(1, 120):
            refuge = doc.createElement('scenario:policeforce')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(agent_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        # ambulanceteam書き出し
        for agent_id in self.choice_agents(1, 120):
            refuge = doc.createElement('scenario:ambulanceteam')
            subnode_attr = doc.createAttribute('scenario:location')
            subnode_attr.value = str(agent_id)
            refuge.setAttributeNode(subnode_attr)
            root.appendChild(refuge)

        f1 = open(self.path, 'w')
        f1.write(doc.toprettyxml())
        f1.close()
