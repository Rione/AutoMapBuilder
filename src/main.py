import os
import sys

sys.path.append(os.getcwd().replace('/src', ''))

from src import CreateMap, GMLWriter, AutoMapBuilder, CreateMapArray

# 20:3
MAP_WIDTH = 50
MAP_HEIGHT = 30
ROAD_WIDTH = 1
# BUILDING_NUMBER = 30

BUILDING_NUMBER = int(MAP_WIDTH * MAP_HEIGHT * 3 / 20)


def main():
    builder = AutoMapBuilder.AutoMapBuilder(MAP_WIDTH, MAP_HEIGHT)
    create_map_array = CreateMapArray.CreateMapArray(MAP_WIDTH, MAP_HEIGHT, ROAD_WIDTH)
    map_array = create_map_array.create(BUILDING_NUMBER)
    builder.calc_nodes(map_array)
    builder.calc_edges(map_array)
    builder.calc_world(map_array)
    builder.calc_road_neighbor()
    builder.calc_building_neighbor()
    writer = GMLWriter.GMLWrite('/home/migly/git/rcrs-server/maps/gml/original/map/map.gml', builder.nodes,
                                builder.edges,
                                builder.buildings, builder.roads)
    writer.write()


if __name__ == '__main__':
    main()
