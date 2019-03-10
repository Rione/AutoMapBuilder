import os
import sys

sys.path.append(os.getcwd().replace('/src', ''))

from src import GMLWriter, AutoMapBuilder, CreateMapArray, ScenarioWriter

# 20:3
MAP_WIDTH = 20
MAP_HEIGHT = 20
ROAD_WIDTH = 1
# BUILDING_NUMBER = 30

BUILDING_NUMBER = int((MAP_WIDTH * MAP_HEIGHT) * 10 / 400)


def main():
    builder = AutoMapBuilder.AutoMapBuilder(MAP_WIDTH, MAP_HEIGHT)
    create_map_array = CreateMapArray.CreateMapArray(MAP_WIDTH, MAP_HEIGHT, ROAD_WIDTH)
    map_array = create_map_array.create(BUILDING_NUMBER)
    builder.calc_nodes(map_array)
    builder.calc_edges(map_array)
    builder.calc_world(map_array)
    builder.calc_road_neighbor()
    builder.calc_building_neighbor()
    gml_writer = GMLWriter.GMLWrite('/home/migly/git/rcrs-server/maps/gml/original/map/map.gml', builder.nodes,
                                    builder.edges,
                                    builder.buildings, builder.roads)
    gml_writer.write()
    scenario_writer = ScenarioWriter.ScenarioWriter('/home/migly/git/rcrs-server/maps/gml/original/map/scenario.xml',
                                                    builder.nodes,
                                                    builder.edges,
                                                    builder.buildings, builder.roads)
    # 10*10のマップを1とした時のシナリオ
    scenario_writer.write((MAP_WIDTH * MAP_HEIGHT) / 100)


if __name__ == '__main__':
    main()
