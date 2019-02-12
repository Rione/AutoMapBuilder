class Stack:
    def __init__(self):
        self.stack = []

    def push(self, e):
        self.stack.append(e)
        return self.stack

    def pop(self):
        return self.stack.pop()
