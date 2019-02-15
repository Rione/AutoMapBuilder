from src import MapReader, Astar

if __name__ == '__main__':
    map = MapReader.MapReader('sakae')
    map.build_graph()

    astar = Astar.Astar(map.world_info.g_nodes)
    print(astar.calc_distance(199165, 200984))
