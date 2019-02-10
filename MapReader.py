import xml.etree.ElementTree as ET


class MapReader:
    node_id = []

    def __init__(self, map_name: str):
        tree = ET.ElementTree(file='./map/' + map_name + '/map/map.gml')
        root = tree.getroot()
        for nodes in root[0]:
            print(nodes[0][0][0].text)
        for edges in root[1]:
            print(dict(edges.attrib).get('{http://www.opengis.net/gml}id'))


kobe = MapReader('kobe')
