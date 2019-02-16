from src import MapReader, Astar
from src import GraphDrawer

if __name__ == '__main__':
    map = MapReader.MapReader('sakae')
    world_info = map.build_map()
    graph_info = map.build_graph()

    drawer = GraphDrawer.GraphDrawer(world_info.g_nodes)

    astar = Astar.Astar(map.world_info.g_nodes)
    result = astar.calc_distance(200984, 201752)

    print(result[0])

    drawer.map_register(graph_info.branch_list)
    drawer.route_register(result[1])
    drawer.show_plt()
