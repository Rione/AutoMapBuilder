import matplotlib.pyplot as plt


class GraphDrawer:

    def __init__(self, g_nodes: dict):
        self.g_nodes = g_nodes

    def reset_plt(self):
        plt.cla()

    def map_register(self, branch_list: dict):
        for id in branch_list:
            # ノード
            plt.scatter(self.g_nodes.get(id).x, self.g_nodes.get(id).y)

            # ブランチ
            for neighbour_branch in branch_list.get(id):
                neighbour = self.g_nodes.get(neighbour_branch.to_node)
                plt.plot([self.g_nodes.get(id).x, neighbour.x],
                         [self.g_nodes.get(id).y, neighbour.y], 'k-')

    def show_plt(self):
        plt.show()
        plt.cla()
