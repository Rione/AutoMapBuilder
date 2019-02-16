import matplotlib.pyplot as plt
from collections import defaultdict


class GraphDrawer:

    def __init__(self, g_nodes: dict):
        self.g_nodes = g_nodes

    def reset_plt(self):
        plt.cla()

    def map_register(self, branch_list: dict):
        for id in branch_list:
            # ノード
            plt.scatter(self.g_nodes.get(id).x, self.g_nodes.get(id).y, color='red')

            # ブランチ
            ##描画済みノードペア格納
            drawed = defaultdict(list)
            for neighbour_branch in branch_list.get(id):
                # neighbourノードを取得
                neighbour = self.g_nodes.get(neighbour_branch.to_node)

                # 描画されたノードペアは除外
                ##キーに登録されている
                if id in drawed:
                    # リストに登録されていない場合
                    if not neighbour.id in drawed[id]:
                        plt.plot([self.g_nodes.get(id).x, neighbour.x],
                                 [self.g_nodes.get(id).y, neighbour.y], 'k-', color='black')
                        drawed[id].append(neighbour.id)

                ##キーに登録されていない
                if not id in drawed:
                    plt.plot([self.g_nodes.get(id).x, neighbour.x],
                             [self.g_nodes.get(id).y, neighbour.y], 'k-', color='black')
                    drawed[id].append(neighbour.id)

    def show_plt(self):
        plt.show()