from src.World import Building, Road
import xml.etree.ElementTree as et
import xml.dom.minidom


class GMLWrite:
    def __init__(self, path: str):
        self.path = path

    def write(self, building_list: dict, road_list: dict):
        nodes = {}
        edges = {}
        for building_id in building_list:
            for edge_id in building_list[building_id].edges:
                f_node = building_list[building_id].edges[edge_id].first
                nodes.setdefault(f_node.id, f_node)
                e_node = building_list[building_id].edges[edge_id].end
                nodes.setdefault(e_node.id, e_node)
                edges.setdefault(edge_id, building_list[building_id].edges[edge_id])

        for road_id in road_list:
            for edge_id in road_list[road_id].edges:
                f_node = road_list[road_id].edges[edge_id].first
                nodes.setdefault(f_node.id, f_node)
                e_node = road_list[road_id].edges[edge_id].end
                nodes.setdefault(e_node.id, e_node)
                edges.setdefault(edge_id, road_list[road_id].edges[edge_id])

        doc = xml.dom.minidom.Document()

        root = doc.createElement(
            'rcr:map xmlns:rcr="urn:roborescue:map:gml" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink"')
        doc.appendChild(root)

        nodelist = doc.createElement('rcr:nodelist')
        root.appendChild(nodelist)
        # node書き出し
        for node_id in nodes:
            Node = doc.createElement('gml:Node gml:id="' + str(node_id) + '"')
            nodelist.appendChild(Node)
            pointProperty = doc.createElement('gml:pointProperty')
            Node.appendChild(pointProperty)
            Point = doc.createElement('gml: Point')
            pointProperty.appendChild(Point)
            coordinates = doc.createElement('gml:coordinates')
            Point.appendChild(coordinates)
            coordinates.appendChild(
                doc.createTextNode(str(float(nodes[node_id].x)) + ',' + str(float(nodes[node_id].y))))

        edgelist = doc.createElement('rcr: edgelist')
        root.appendChild(edgelist)
        # edge書き出し
        for edge_id in edges:
            Edge = doc.createElement('gml:Edge gml:id="' + str(edge_id) + '"')
            edgelist.appendChild(Edge)
            directedNode = doc.createElement('orientation="-" xlink:href="#' + str(edges[edge_id].first.id) + '"')
            Edge.appendChild(directedNode)
            directedNode = doc.createElement('orientation="+" xlink:href="#' + str(edges[edge_id].end.id) + '"')
            Edge.appendChild(directedNode)

        print(doc.toprettyxml())
