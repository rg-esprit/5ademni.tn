<?php

namespace App\Form;

use App\Entity\Contrat;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints\NotBlank;

class ContratType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        if ($options['is_admin']) {
            $builder
                ->add('clientId', IntegerType::class, [
                    'label' => 'ID Client',
                    'attr' => ['class' => 'bento-input'],
                ])
                ->add('freelancerId', IntegerType::class, [
                    'label' => 'ID Freelancer',
                    'attr' => ['class' => 'bento-input'],
                ])
                ->add('titre', TextType::class, [
                    'label' => 'Titre du Contrat',
                    'attr' => ['class' => 'bento-input'],
                ]);
        } else {
            $builder
                ->add('clientId', HiddenType::class)
                ->add('freelancerId', HiddenType::class, [
                    'required' => false,
                ]);

            if ($options['use_job_selector']) {
                $builder->add('titre', ChoiceType::class, [
                    'label' => 'Offre acceptee',
                    'choices' => $options['job_choices'],
                    'placeholder' => '-- Choisir une offre acceptee --',
                    'mapped' => false,
                    'constraints' => [
                        new NotBlank(['message' => 'Veuillez selectionner une offre avec un freelancer accepte.']),
                    ],
                    'attr' => [
                        'class' => 'bento-input',
                        'data-contract-job-selector' => '1',
                    ],
                ]);
            } else {
                $builder->add('titre', TextType::class, [
                    'label' => 'Titre du Contrat',
                    'attr' => ['class' => 'bento-input'],
                ]);
            }
        }

        $builder
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'required' => false,
                'attr' => ['class' => 'bento-input-textarea', 'rows' => 4],
            ])
            ->add('prix', NumberType::class, [
                'label' => 'Prix',
                'attr' => ['class' => 'bento-input'],
            ])
            ->add('dateContrat', DateType::class, [
                'widget' => 'single_text',
                'label' => 'Date du contrat',
                'attr' => ['class' => 'bento-input'],
            ])
            ->add('numTelephone', TextType::class, [
                'label' => 'Téléphone',
                'required' => false,
                'attr' => ['class' => 'bento-input'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Contrat::class,
            'is_admin' => false,
            'job_choices' => [],
            'use_job_selector' => false,
        ]);
    }
}
