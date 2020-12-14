echo
@chcp 65001
@set id=%1
@echo Launch LFD%id%:
@cd out/production/18749project/
java detectors.LFD %id%
@pause