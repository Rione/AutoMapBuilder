import xml.etree.ElementTree as ET
import Node


class MapReader:
    def __init__(self, map_name: str):
        self.node = []
        tree = ET.ElementTree(file='./map/' + map_name + '/map/map.gml')
        root = tree.getroot()
        for nodes in root[0]:
            id = nodes.attrib.get('{http://www.opengis.net/gml}id')
            x = float(str(nodes[0][0][0].text).split(',')[0])
            y = float(str(nodes[0][0][0].text).split(',')[1])
            self.node.append(Node.Node(id, x, y))

        for edges in root[1]:
            id = dict(edges.attrib).get('{http://www.opengis.net/gml}id')
            print(edges[1].attrib)


kobe = MapReader('kobe')
