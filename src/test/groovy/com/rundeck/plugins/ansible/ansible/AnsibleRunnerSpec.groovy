package com.rundeck.plugins.ansible.ansible

import com.rundeck.plugins.ansible.util.ProcessExecutor
import spock.lang.Specification

import java.nio.file.Path

class AnsibleRunnerSpec extends Specification{

    def setup(){

    }

    def "wrong extra vars format"() {
        given:
        String playbook = "test"
        String privateKey = "privateKey"
        String extraVars = "123dxxx"

        def runner = AnsibleRunner.playbookInline(playbook)
        runner.encryptExtraVars(true)
        runner.sshPrivateKey(privateKey)
        runner.extraVars(extraVars)

        def process = Mock(Process){
            waitFor() >> 0
            getInputStream()>> new ByteArrayInputStream("".getBytes())
            getOutputStream() >> new ByteArrayOutputStream()
            getErrorStream() >> new ByteArrayInputStream("".getBytes())
        }

        def processExecutor = Mock(ProcessExecutor){
            run()>>process
        }

        def processBuilder = Mock(ProcessExecutor.ProcessExecutorBuilder){
            build() >> processExecutor
        }

        def ansibleVault = Mock(AnsibleVault){
            checkAnsibleVault() >> true
            getVaultPasswordScriptFile() >> new File("vault-script-client.py")
        }


        runner.processExecutorBuilder(processBuilder)
        runner.ansibleVault(ansibleVault)

        when:
        runner.build().run()

        then:
        def e = thrown(Exception)
        e.message.contains("cannot parse extra var values")

    }


    def "test encrypt extra vars"() {

        given:

        String playbook = "test"
        String privateKey = "privateKey"
        String extraVars = "test: 123\ntest2: 456"


        def runnerBuilder = AnsibleRunner.builder()
        runnerBuilder.type(AnsibleRunner.AnsibleCommand.PlaybookPath)
        runnerBuilder.playbook(playbook)
        runnerBuilder.encryptExtraVars(true)
        runnerBuilder.sshPrivateKey(privateKey)
        runnerBuilder.extraVars(extraVars)

        def process = Mock(Process){
            waitFor() >> 0
            getInputStream()>> new ByteArrayInputStream("".getBytes())
            getOutputStream() >> new ByteArrayOutputStream()
            getErrorStream() >> new ByteArrayInputStream("".getBytes())
        }

        def processExecutor = Mock(ProcessExecutor){
            run()>>process
        }

        def processBuilder = Mock(ProcessExecutor.ProcessExecutorBuilder){
            build() >> processExecutor
        }

        def ansibleVault = Mock(AnsibleVault){
            checkAnsibleVault() >> true
            getVaultPasswordScriptFile() >> new File("vault-script-client.py")
        }

        runnerBuilder.processExecutorBuilder(processBuilder)
        runnerBuilder.ansibleVault(ansibleVault)

        when:
        def result = runnerBuilder.build().run()

        then:

        2 * ansibleVault.encryptVariable(_,_) >> "!vault | value"
        1* processBuilder.procArgs(_) >> { args ->
            def cmd = args[0]
            assert cmd.contains("--vault-id")
            assert cmd.contains("internal-encrypt@" + ansibleVault.getVaultPasswordScriptFile().absolutePath)
        }
        result == 0
    }


    def "test clean temporary directory"(){
        given:
        def tmpDirectory = File.createTempDir("ansible-runner-test-", "tmp")
        String playbook = "test"
        String privateKey = "privateKey"
        String extraVars = "test: 123\ntest2: 456"

        def runner = AnsibleRunner.playbookInline(playbook)
        runner.encryptExtraVars(true)
        runner.tempDirectory(Path.of(tmpDirectory.absolutePath))
        runner.sshPrivateKey(privateKey)
        runner.extraVars(extraVars)

        def process = Mock(Process){
            waitFor() >> 0
            getInputStream()>> new ByteArrayInputStream("".getBytes())
            getOutputStream() >> new ByteArrayOutputStream()
            getErrorStream() >> new ByteArrayInputStream("".getBytes())
        }

        def processExecutor = Mock(ProcessExecutor){
            run()>>process
        }

        def processBuilder = Mock(ProcessExecutor.ProcessExecutorBuilder){
            build() >> processExecutor
        }

        def ansibleVault = Mock(AnsibleVault){
            checkAnsibleVault() >> true
            getVaultPasswordScriptFile() >> new File("vault-script-client.py")
            encryptVariable(_,_) >> { throw new Exception("Error encrypting value") }
        }

        runner.processExecutorBuilder(processBuilder)
        runner.ansibleVault(ansibleVault)

        when:
        runner.build().run()

        then:
        def e = thrown(Exception)
        e.message.contains("cannot parse extra var values")

        !tmpDirectory.exists()
    }

    def "test clean temporary files when a exception is trigger"(){
        given:

        String playbook = "test"
        String privateKey = "privateKey"
        String extraVars = "test: 123\ntest2: 456"

        def runnerBuilder = AnsibleRunner.playbookInline(playbook)
        runnerBuilder.encryptExtraVars(true)
        runnerBuilder.sshPrivateKey(privateKey)
        runnerBuilder.extraVars(extraVars)

        def process = Mock(Process){
            waitFor() >> 0
            getInputStream()>> new ByteArrayInputStream("".getBytes())
            getOutputStream() >> new ByteArrayOutputStream()
            getErrorStream() >> new ByteArrayInputStream("".getBytes())
        }

        def processExecutor = Mock(ProcessExecutor){
            run()>>process
        }

        def processBuilder = Mock(ProcessExecutor.ProcessExecutorBuilder){
            build() >> processExecutor
        }


        def ansibleVault = Mock(AnsibleVault){
            checkAnsibleVault() >> true
            getVaultPasswordScriptFile() >> new File("vault-script-client.py")
            encryptVariable(_,_) >> { throw new Exception("Error encrypting value") }
        }

        runnerBuilder.processExecutorBuilder(processBuilder)
        runnerBuilder.ansibleVault(ansibleVault)

        when:
        AnsibleRunner runner = runnerBuilder.build()
        runner.run()

        then:
        def e = thrown(Exception)
        e.message.contains("cannot parse extra var values")

        !runner.getTempPlaybook().exists()


    }

    def "test clean temporary files when process finished"(){
        given:

        String playbook = "test"
        String privateKey = "privateKey"
        String extraVars = "test: 123\ntest2: 456"

        def runnerBuilder = AnsibleRunner.playbookInline(playbook)
        runnerBuilder.encryptExtraVars(true)
        runnerBuilder.sshPrivateKey(privateKey)
        runnerBuilder.extraVars(extraVars)

        def process = Mock(Process){
            waitFor() >> 0
            getInputStream()>> new ByteArrayInputStream("".getBytes())
            getOutputStream() >> new ByteArrayOutputStream()
            getErrorStream() >> new ByteArrayInputStream("".getBytes())
        }

        def processExecutor = Mock(ProcessExecutor){
            run()>>process
        }

        def processBuilder = Mock(ProcessExecutor.ProcessExecutorBuilder){
            build() >> processExecutor
        }


        def ansibleVault = Mock(AnsibleVault){
            checkAnsibleVault() >> true
            getVaultPasswordScriptFile() >> new File("vault-script-client.py")
        }

        runnerBuilder.processExecutorBuilder(processBuilder)
        runnerBuilder.ansibleVault(ansibleVault)

        when:
        AnsibleRunner runner = runnerBuilder.build()
        def result = runner.run()

        then:
        2 * ansibleVault.encryptVariable(_,_) >> "!vault | value"
        result == 0
        !runner.getTempPlaybook().exists()
        !runner.getTempPkFile().exists()
        !runner.getTempVarsFile().exists()


    }
}
