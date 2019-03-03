import os
import sys

sys.path.append(os.getcwd().replace('/src', ''))

from src import CreateMap, GMLWriter, AutoMapBuilder, CreateMapArray


def main():
    builder = AutoMapBuilder.AutoMapBuilder()
    create_map_array = CreateMapArray.CreateMapArray(10, 10)
    map_array = create_map_array.create(15)
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
