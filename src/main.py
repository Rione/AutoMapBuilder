from src import CreateMap, GMLWriter, AutoMapBuilder


def main():
    builder = AutoMapBuilder.AutoMapBuilder()
    map_array = builder.make_map_array(25, 20, 20)
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
