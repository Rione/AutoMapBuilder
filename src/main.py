from src import MapReader, Astar
from src import GraphDrawer

if __name__ == '__main__':
    map = MapReader.MapReader('sakae')
    world_info = map.build_map()
    graph_info = map.build_graph()

    drawer = GraphDrawer.GraphDrawer(world_info.g_nodes)

    drawer.map_register(graph_info.branch_list)
    drawer.show_plt()

    astar = Astar.Astar(map.world_info.g_nodes)
    print(astar.calc_distance(200984, 199165))
