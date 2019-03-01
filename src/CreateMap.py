import numpy as np

MAP_WIDTH = 1000
MAP_HEIGHT = 1000


# 空白:-1
# 道:0
# 建物:1~

# 最初に配列にランダムに拠点を作る
# その時に別にIDとノード情報を保持するテーブルを作成
# 次に陣地をうばうように建物を広げていく
# その際にノード情報を更新していく(無い座標は追加、ある座標は削除)

class CreateMap:
    def __init__(self):
        self.map_array = np.full((MAP_WIDTH, MAP_HEIGHT), -1)

    # 指定場所を埋める

    # maparrayの中心からある範囲まで占領


create = CreateMap()
print(create.map_array)
