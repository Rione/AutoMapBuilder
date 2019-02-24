from datetime import datetime
import matplotlib.pyplot as plt
from collections import defaultdict


class GraphDrawer:

    def __init__(self, g_nodes: dict):
        self.g_nodes = g_nodes

    def reset_plt(self):
        plt.cla()

    def node_register(self, id: int):
        plt.scatter(self.g_nodes.get(id).x, self.g_nodes.get(id).y, s=600, c="yellow", marker="*", alpha=0.5,
                    linewidths="2", edgecolors="orange")

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

    def route_register(self, route: list):
        # routeIDからノードとエッジを描画
        for i in range(len(route)):
            ##ノード描画
            plt.scatter(self.g_nodes.get(route[i]).x, self.g_nodes.get(route[i]).y, color='blue')
            ##エッジ描画（ただし終端は除外）
            if not i == len(route) - 1:
                plt.plot([self.g_nodes.get(route[i]).x, self.g_nodes.get(route[i + 1]).x],
                         [self.g_nodes.get(route[i]).y, self.g_nodes.get(route[i + 1]).y], 'k-', color='blue')

    def show_plt(self):
        #plt.savefig('./image/test' + datetime.now().strftime("%Y%m%d-%H%M%S") + '.png')
        plt.show()
