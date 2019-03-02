from src import CreateMap, GMLWriter, AutoMapBuilder


def main():
    # create_map = CreateMap.CreateMap()
    builder = AutoMapBuilder.AutoMapBuilder()
    map_array = builder.make_map_array(25, 20, 20)
    # gml_write = GMLWriter.GMLWrite('/home/migly/git/rcrs-server/maps/gml/original/map/map.gml')
    # world = create_map.create_world_map(25)
    # print(world[0])
    # print(world[1])

    # gml_write.write(world[0], world[1])


if __name__ == '__main__':
    main()
