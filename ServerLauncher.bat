echo
@chcp 65001
@set id=%1
@echo Launch S%id%:
@cd out/production/18749project/
java servers.ServerReplica %id%
@pause