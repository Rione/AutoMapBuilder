from src import CreateMap, GMLWriter


def main():
    create_map = CreateMap.CreateMap()
    gml_write = GMLWriter.GMLWrite('test.xml')
    world = create_map.create_world_map(25)
    print(world[0])
    print(world[1])


if __name__ == '__main__':
    main()
